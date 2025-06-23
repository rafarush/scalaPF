package services

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import models.Center

class CenterService[F[_]: MonadCancelThrow] (xa: Transactor[F]) {

  def create(center: Center): ConnectionIO[Either[String, Center]] = {
    for {
      exists <- isCenterCreated
      result <- if (exists) {
        MonadCancelThrow[ConnectionIO].pure(Left("Center already exists"))
      } else if (!center.isValid) {
        MonadCancelThrow[ConnectionIO].pure(Left("Invalid center data"))
      } else {
        val insert = sql"""
          INSERT INTO center (centercode, centername, postaladdress, phone, centeremail,
                             generaldirectorname, hrmanager, accountingmanager, secretaryname, logo)
          VALUES (${center.centerCode}, ${center.centerName}, ${center.postalAddress},
                  ${center.phone}, ${center.centerEmail}, ${center.generalDirectorName},
                  ${center.hrManager}, ${center.accountingManager}, ${center.secretaryName}, ${center.logo})
        """.update

        insert.run.map(_ => Right(center))
      }
    } yield result
  }

  def findById(centerCode: String): ConnectionIO[Option[Center]] = {
    sql"""
      SELECT centercode, centername, postaladdress, phone, centeremail,
             generaldirectorname, hrmanager, accountingmanager, secretaryname, logo
      FROM center
      WHERE centercode = $centerCode
    """.query[Center].option
  }

  def findFirst: ConnectionIO[Option[Center]] = {
    sql"""
      SELECT centercode, centername, postaladdress, phone, centeremail,
             generaldirectorname, hrmanager, accountingmanager, secretaryname, logo
      FROM center
      LIMIT 1
    """.query[Center].option
  }

  def update(center: Center): ConnectionIO[Either[String, Center]] = {
    val updateSql = sql"""
      UPDATE center 
      SET centername = ${center.centerName}, postaladdress = ${center.postalAddress},
          phone = ${center.phone}, centeremail = ${center.centerEmail},
          generaldirectorname = ${center.generalDirectorName}, hrmanager = ${center.hrManager},
          accountingmanager = ${center.accountingManager}, secretaryname = ${center.secretaryName},
          logo = ${center.logo}
      WHERE centercode = ${center.centerCode}
    """.update

    updateSql.run.map { affected =>
      if (affected > 0) Right(center)
      else Left(s"Center with code ${center.centerCode} not found")
    }
  }

  def delete(centerCode: String): ConnectionIO[Either[String, Unit]] = {
    sql"DELETE FROM center WHERE centercode = $centerCode".update.run.map { affected =>
      if (affected > 0) Right(())
      else Left(s"Center with code $centerCode not found")
    }
  }

  private def isCenterCreated: ConnectionIO[Boolean] = {
    sql"SELECT COUNT(*) FROM center".query[Int].unique.map(_ > 0)
  }
}

object CenterService {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): CenterService[F] =
    new CenterService(xa)
}
