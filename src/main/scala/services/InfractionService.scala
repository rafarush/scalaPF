package services

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import models.Infraction
import java.time.LocalDate
import doobie.postgres.implicits._


class InfractionService[F[_]: MonadCancelThrow] (xa: Transactor[F]) {

  def create(infraction: Infraction): ConnectionIO[Either[String, Infraction]] = {
    val insert = sql"""
      INSERT INTO infraction (infractioncode, licenseid, violationtype, date, location, 
                             description, points, ispaid)
      VALUES (${infraction.infractionCode}, ${infraction.licenseId}, ${infraction.violationType},
              ${infraction.date}, ${infraction.location}, ${infraction.description},
              ${infraction.points}, ${infraction.isPaid})
    """.update

    for {
      exists <- isDuplicated(infraction.infractionCode)
      result <- if (exists) {
        MonadCancelThrow[ConnectionIO].pure(Left("Infraction already exists"))
      } else {
        insert.run.map(_ => Right(infraction))
      }
    } yield result
  }

  def findById(infractionCode: String): ConnectionIO[Option[Infraction]] = {
    sql"""
      SELECT infractioncode, licenseid, violationtype, date, location, description, points, ispaid
      FROM infraction
      WHERE infractioncode = $infractionCode
    """.query[Infraction].option
  }

  def findAll: ConnectionIO[List[Infraction]] = {
    sql"""
      SELECT infractioncode, licenseid, violationtype, date, location, description, points, ispaid
      FROM infraction
    """.query[Infraction].to[List]
  }

  def findRecent: ConnectionIO[List[Infraction]] = {
    val oneWeekAgo = LocalDate.now().minusWeeks(1)
    sql"""
      SELECT infractioncode, licenseid, violationtype, date, location, description, points, ispaid
      FROM infraction
      WHERE date BETWEEN $oneWeekAgo AND CURRENT_DATE
    """.query[Infraction].to[List]
  }

  def findByDateRange(startDate: LocalDate, endDate: LocalDate): ConnectionIO[List[Infraction]] = {
    sql"""
      SELECT infractioncode, licenseid, violationtype, date, location, description, points, ispaid
      FROM infraction
      WHERE date BETWEEN $startDate AND $endDate
    """.query[Infraction].to[List]
  }

  def findConsolidatedByYear(year: Int): ConnectionIO[List[(String, Int, Int, Int, Int)]] = {
    sql"""
      SELECT violationtype, COUNT(*) as totalInfractions,
             SUM(points) as totalPoints,
             SUM(CASE WHEN ispaid THEN 1 ELSE 0 END) as totalPaid,
             SUM(CASE WHEN NOT ispaid THEN 1 ELSE 0 END) as totalPending
      FROM infraction
      WHERE EXTRACT(YEAR FROM date) = $year
      GROUP BY violationtype
      ORDER BY violationtype
    """.query[(String, Int, Int, Int, Int)].to[List]
  }

  def update(infraction: Infraction): ConnectionIO[Either[String, Infraction]] = {
    val updateSql = sql"""
      UPDATE infraction 
      SET licenseid = ${infraction.licenseId}, violationtype = ${infraction.violationType},
          date = ${infraction.date}, location = ${infraction.location},
          description = ${infraction.description}, points = ${infraction.points},
          ispaid = ${infraction.isPaid}
      WHERE infractioncode = ${infraction.infractionCode}
    """.update

    updateSql.run.map { affected =>
      if (affected > 0) Right(infraction)
      else Left(s"Infraction with code ${infraction.infractionCode} not found")
    }
  }

  def delete(infractionCode: String): ConnectionIO[Either[String, Unit]] = {
    sql"DELETE FROM infraction WHERE infractioncode = $infractionCode".update.run.map { affected =>
      if (affected > 0) Right(())
      else Left(s"Infraction with code $infractionCode not found")
    }
  }

  def count: ConnectionIO[Int] = {
    sql"SELECT COUNT(*) FROM infraction".query[Int].unique
  }

  private def isDuplicated(infractionCode: String): ConnectionIO[Boolean] = {
    sql"SELECT COUNT(*) FROM infraction WHERE infractioncode = $infractionCode"
      .query[Int]
      .unique
      .map(_ > 0)
  }
}

object InfractionService {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): InfractionService[F] =
    new InfractionService(xa)
}