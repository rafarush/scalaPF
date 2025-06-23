package utils

import cats.effect._
import doobie._
import doobie.implicits._

object DatabaseMigrations {

  def createTables: ConnectionIO[Unit] = {
    val createCenterTable = sql"""
      CREATE TABLE IF NOT EXISTS center (
        centercode VARCHAR(50) PRIMARY KEY,
        centername VARCHAR(255) NOT NULL,
        postaladdress VARCHAR(500),
        phone VARCHAR(20),
        centeremail VARCHAR(255),
        generaldirectorname VARCHAR(255),
        hrmanager VARCHAR(255),
        accountingmanager VARCHAR(255),
        secretaryname VARCHAR(255),
        logo VARCHAR(500)
      )
    """.update.run

    val createDriverTable = sql"""
      CREATE TABLE IF NOT EXISTS driver (
        driverid VARCHAR(50) PRIMARY KEY,
        firstname VARCHAR(255) NOT NULL,
        lastname VARCHAR(255) NOT NULL,
        birthdate DATE NOT NULL,
        address VARCHAR(500),
        phone VARCHAR(20),
        email VARCHAR(255)
      )
    """.update.run

    val createLicenseTable = sql"""
      CREATE TABLE IF NOT EXISTS license (
        licenseid VARCHAR(50) PRIMARY KEY,
        driverid VARCHAR(50) NOT NULL,
        licensetype VARCHAR(10) NOT NULL,
        issuedate DATE NOT NULL,
        expirationdate DATE NOT NULL,
        restrictions TEXT,
        renewed BOOLEAN DEFAULT FALSE,
        licensestatus VARCHAR(50),
        FOREIGN KEY (driverid) REFERENCES driver(driverid)
      )
    """.update.run

    val createInfractionTable = sql"""
      CREATE TABLE IF NOT EXISTS infraction (
        infractioncode VARCHAR(50) PRIMARY KEY,
        licenseid VARCHAR(50) NOT NULL,
        violationtype VARCHAR(255) NOT NULL,
        date DATE NOT NULL,
        location VARCHAR(500),
        description TEXT,
        points INTEGER DEFAULT 0,
        ispaid BOOLEAN DEFAULT FALSE,
        FOREIGN KEY (licenseid) REFERENCES license(licenseid)
      )
    """.update.run

    val createTestTable = sql"""
      CREATE TABLE IF NOT EXISTS test (
        testcode VARCHAR(50) PRIMARY KEY,
        testtype VARCHAR(50) NOT NULL,
        date DATE NOT NULL,
        result BOOLEAN NOT NULL,
        entityname VARCHAR(255),
        examinername VARCHAR(255),
        driverid VARCHAR(50) NOT NULL,
        licensetype VARCHAR(10),
        FOREIGN KEY (driverid) REFERENCES driver(driverid)
      )
    """.update.run

    val createRelatedEntityTable = sql"""
      CREATE TABLE IF NOT EXISTS relatedentity (
        entityname VARCHAR(255) PRIMARY KEY,
        entitytype VARCHAR(100),
        address VARCHAR(500),
        phone VARCHAR(20),
        contactemail VARCHAR(255),
        directorname VARCHAR(255),
        centercode VARCHAR(50),
        FOREIGN KEY (centercode) REFERENCES center(centercode)
      )
    """.update.run

    for {
      _ <- createCenterTable
      _ <- createDriverTable
      _ <- createLicenseTable
      _ <- createInfractionTable
      _ <- createTestTable
      _ <- createRelatedEntityTable
    } yield ()
  }

  def insertSampleData: ConnectionIO[Unit] = {
    val insertCenter = sql"""
      INSERT INTO center (centercode, centername, postaladdress, phone, centeremail, 
                         generaldirectorname, hrmanager, accountingmanager, secretaryname, logo)
      VALUES ('CTR001', 'Main Driving Center', '123 Center St', '555-0100', 'main@center.com',
              'John Director', 'Jane HR', 'Bob Accounting', 'Alice Secretary', 'logo.png')
      ON CONFLICT (centercode) DO NOTHING
    """.update.run

    val insertDriver = sql"""
      INSERT INTO driver (driverid, firstname, lastname, birthdate, address, phone, email)
      VALUES ('04101968006', 'Anthony', 'Perdomo', '1990-05-15', '456 Driver Ave', '555-0200', 'anthony@email.com')
      ON CONFLICT (driverid) DO NOTHING
    """.update.run

    val insertLicense = sql"""
      INSERT INTO license (licenseid, driverid, licensetype, issuedate, expirationdate, 
                          restrictions, renewed, licensestatus)
      VALUES ('3', '04101968006', 'A', '2023-01-01', '2028-01-01', 'None', false, 'Active')
      ON CONFLICT (licenseid) DO NOTHING
    """.update.run

    val insertTest = sql"""
      INSERT INTO test (testcode, testtype, date, result, entityname, examinername, driverid, licensetype)
      VALUES ('test01', 'Teorico', '2020-08-13', true, 'Autoescuela Central', 'Dr. Smith', '04101968006', 'A')
      ON CONFLICT (testcode) DO NOTHING
    """.update.run

    val insertRelatedEntity = sql"""
      INSERT INTO relatedentity (entityname, entitytype, address, phone, contactemail, directorname, centercode)
      VALUES ('1', 'Autoescuela', '789 Entity St', '555-0300', 'entity@email.com', 'Entity Director', 'CTR001')
      ON CONFLICT (entityname) DO NOTHING
    """.update.run

    for {
      _ <- insertCenter
      _ <- insertDriver
      _ <- insertLicense
      _ <- insertTest
      _ <- insertRelatedEntity
    } yield ()
  }
}
