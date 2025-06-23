package services

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import models.Driver
import java.time.LocalDate
import doobie.postgres.implicits._


class DriverService[F[_]: MonadCancelThrow] (xa: Transactor[F]) {

  def create(driver: Driver): ConnectionIO[Either[String, Driver]] = {
    val insert = sql"""
      INSERT INTO driver (driverid, firstname, lastname, birthdate, address, phone, email)
      VALUES (${driver.driverId}, ${driver.firstName}, ${driver.lastName},
              ${driver.birthDate}, ${driver.address}, ${driver.phone}, ${driver.email})
    """.update

    for {
      exists <- isDuplicated(driver.driverId)
      result <- if (exists) {
        MonadCancelThrow[ConnectionIO].pure(Left("Driver already exists"))
      } else {
        insert.run.map(_ => Right(driver))
      }
    } yield result
  }

  def findById(driverId: String): ConnectionIO[Option[Driver]] = {
    sql"""
      SELECT driverid, firstname, lastname, birthdate, address, phone, email
      FROM driver
      WHERE driverid = $driverId
    """.query[Driver].option
  }

  def findAll: ConnectionIO[List[Driver]] = {
    sql"""
      SELECT driverid, firstname, lastname, birthdate, address, phone, email
      FROM driver
    """.query[Driver].to[List]
  }

  def update(driver: Driver): ConnectionIO[Either[String, Driver]] = {
    val updateSql = sql"""
      UPDATE driver
      SET firstname = ${driver.firstName}, lastname = ${driver.lastName},
          birthdate = ${driver.birthDate}, address = ${driver.address},
          phone = ${driver.phone}, email = ${driver.email}
      WHERE driverid = ${driver.driverId}
    """.update

    updateSql.run.map { affected =>
      if (affected > 0) Right(driver)
      else Left(s"Driver with ID ${driver.driverId} not found")
    }
  }

  def delete(driverId: String): ConnectionIO[Either[String, Unit]] = {
    sql"DELETE FROM driver WHERE driverid = $driverId".update.run.map { affected =>
      if (affected > 0) Right(())
      else Left(s"Driver with ID $driverId not found")
    }
  }

  def count: ConnectionIO[Int] = {
    sql"SELECT COUNT(*) FROM driver".query[Int].unique
  }

  private def isDuplicated(driverId: String): ConnectionIO[Boolean] = {
    sql"SELECT COUNT(*) FROM driver WHERE driverid = $driverId"
      .query[Int]
      .unique
      .map(_ > 0)
  }
}

object DriverService {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): DriverService[F] =
    new DriverService(xa)
}
