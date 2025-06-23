package models

import java.time.LocalDate

case class Driver(
                   driverId: String,
                   firstName: String,
                   lastName: String,
                   birthDate: LocalDate,
                   address: String,
                   phone: String,
                   email: String
                 ) {
  def fullName: String = s"$firstName $lastName"

  def isValid: Boolean =
    driverId.nonEmpty &&
      firstName.nonEmpty &&
      lastName.nonEmpty &&
      email.contains("@")
}

object Driver {
  def create(
              driverId: String,
              firstName: String,
              lastName: String,
              birthDate: LocalDate,
              address: String,
              phone: String,
              email: String
            ): Either[String, Driver] = {
    val driver = Driver(driverId, firstName, lastName, birthDate, address, phone, email)
    if (driver.isValid) Right(driver)
    else Left("Invalid driver data")
  }
}
