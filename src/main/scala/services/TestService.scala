package services

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import models.Test
import java.time.LocalDate
import doobie.postgres.implicits._

class TestService[F[_]: MonadCancelThrow] (xa: Transactor[F]) {

  def create(test: Test): ConnectionIO[Either[String, Test]] = {
    val insert = sql"""
      INSERT INTO test (testcode, testtype, date, result, entityname, examinername, driverid, licensetype)
      VALUES (${test.testCode}, ${test.testType}, ${test.date}, ${test.result},
              ${test.entityName}, ${test.examinerName}, ${test.driverId}, ${test.licenseType})
    """.update

    insert.run.map(_ => Right(test))
  }

  def findById(testCode: String): ConnectionIO[Option[Test]] = {
    sql"""
      SELECT testcode, testtype, date, result, entityname, examinername, driverid, licensetype
      FROM test
      WHERE testcode = $testCode
    """.query[Test].option
  }

  def findAll: ConnectionIO[List[Test]] = {
    sql"""
      SELECT testcode, testtype, date, result, entityname, examinername, driverid, licensetype
      FROM test
    """.query[Test].to[List]
  }

  def findByDateRange(startDate: LocalDate, endDate: LocalDate): ConnectionIO[List[Test]] = {
    sql"""
      SELECT testcode, testtype, date, result, entityname, examinername, driverid, licensetype
      FROM test
      WHERE date BETWEEN $startDate AND $endDate
    """.query[Test].to[List]
  }

  def findApprovedValidTests(driverId: String, licenseType: String): ConnectionIO[List[Test]] = {
    val sixMonthsAgo = LocalDate.now().minusMonths(6)
    sql"""
      SELECT testcode, testtype, date, result, entityname, examinername, driverid, licensetype
      FROM test
      WHERE driverid = $driverId 
      AND licensetype = $licenseType
      AND date BETWEEN $sixMonthsAgo AND CURRENT_DATE
      AND result = true
    """.query[Test].to[List]
  }

  def update(test: Test): ConnectionIO[Either[String, Test]] = {
    val updateSql = sql"""
      UPDATE test 
      SET testtype = ${test.testType}, date = ${test.date}, result = ${test.result},
          entityname = ${test.entityName}, examinername = ${test.examinerName},
          driverid = ${test.driverId}, licensetype = ${test.licenseType}
      WHERE testcode = ${test.testCode}
    """.update

    updateSql.run.map { affected =>
      if (affected > 0) Right(test)
      else Left(s"Test with code ${test.testCode} not found")
    }
  }

  def delete(testCode: String): ConnectionIO[Either[String, Unit]] = {
    sql"DELETE FROM test WHERE testcode = $testCode".update.run.map { affected =>
      if (affected > 0) Right(())
      else Left(s"Test with code $testCode not found")
    }
  }

  def count: ConnectionIO[Int] = {
    sql"SELECT COUNT(*) FROM test".query[Int].unique
  }
}

object TestService {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): TestService[F] =
    new TestService(xa)
}