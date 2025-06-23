package services

import cats.effect._
import cats.implicits._
import doobie.postgres.implicits._
import doobie._
import doobie.implicits._
import models.{License, Test}
import java.time.LocalDate

class LicenseService[F[_]: MonadCancelThrow] (xa: Transactor[F]) {

  def create(license: License): ConnectionIO[Either[String, License]] = {
    for {
      isValid <- validateLicense(license)
      result <- if (isValid) {
        val insert = sql"""
          INSERT INTO license (licenseid, driverid, licensetype, issuedate, expirationdate, 
                              restrictions, renewed, licensestatus)
          VALUES (${license.licenseId}, ${license.driverId}, ${license.licenseType},
                  ${license.issueDate}, ${license.expirationDate}, ${license.restrictions},
                  ${license.renewed}, ${license.licenseStatus})
        """.update

        insert.run.map(_ => Right(license))
      } else {
        MonadCancelThrow[ConnectionIO].pure(Left("License validation failed"))
      }
    } yield result
  }

  def findById(licenseId: String): ConnectionIO[Option[License]] = {
    sql"""
      SELECT licenseid, driverid, licensetype, issuedate, expirationdate,
             restrictions, renewed, licensestatus
      FROM license
      WHERE licenseid = $licenseId
    """.query[License].option
  }

  def findAll: ConnectionIO[List[License]] = {
    sql"""
      SELECT licenseid, driverid, licensetype, issuedate, expirationdate,
             restrictions, renewed, licensestatus
      FROM license
    """.query[License].to[List]
  }

  def findAlmostExpired: ConnectionIO[List[License]] = {
    val sixMonthsFromNow = LocalDate.now().plusMonths(6)
    sql"""
      SELECT licenseid, driverid, licensetype, issuedate, expirationdate,
             restrictions, renewed, licensestatus
      FROM license
      WHERE expirationdate BETWEEN CURRENT_DATE AND $sixMonthsFromNow
    """.query[License].to[List]
  }

  def findExpired(startDate: LocalDate, endDate: LocalDate): ConnectionIO[List[License]] = {
    sql"""
      SELECT licenseid, driverid, licensetype, issuedate, expirationdate,
             restrictions, renewed, licensestatus
      FROM license
      WHERE expirationdate BETWEEN $startDate AND $endDate
      AND expirationdate < CURRENT_DATE
    """.query[License].to[List]
  }

  def update(license: License): ConnectionIO[Either[String, License]] = {
    val updateSql = sql"""
      UPDATE license 
      SET driverid = ${license.driverId}, licensetype = ${license.licenseType},
          issuedate = ${license.issueDate}, expirationdate = ${license.expirationDate},
          restrictions = ${license.restrictions}, renewed = ${license.renewed},
          licensestatus = ${license.licenseStatus}
      WHERE licenseid = ${license.licenseId}
    """.update

    updateSql.run.map { affected =>
      if (affected > 0) Right(license)
      else Left(s"License with ID ${license.licenseId} not found")
    }
  }

  def delete(licenseId: String): ConnectionIO[Either[String, Unit]] = {
    sql"DELETE FROM license WHERE licenseid = $licenseId".update.run.map { affected =>
      if (affected > 0) Right(())
      else Left(s"License with ID $licenseId not found")
    }
  }

  def count: ConnectionIO[Int] = {
    sql"SELECT COUNT(*) FROM license".query[Int].unique
  }

  private def validateLicense(license: License): ConnectionIO[Boolean] = {
    // Check if all required tests are passed
    val testService = TestService(xa)
    testService.findApprovedValidTests(license.driverId, license.licenseType).map { tests =>
      val hasTheoretical = tests.exists(_.testType.equalsIgnoreCase("Teorico"))
      val hasPractical = tests.exists(_.testType.equalsIgnoreCase("Practico"))
      val hasMedical = tests.exists(_.testType.equalsIgnoreCase("Medico"))

      hasTheoretical && hasPractical && hasMedical && license.isValid
    }
  }
}

object LicenseService {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): LicenseService[F] =
    new LicenseService(xa)
}