package models

import java.time.LocalDate

case class License(
                    licenseId: String,
                    driverId: String,
                    licenseType: String,
                    issueDate: LocalDate,
                    expirationDate: LocalDate,
                    restrictions: String,
                    renewed: Boolean,
                    licenseStatus: String
                  ) {
  def isExpired: Boolean = expirationDate.isBefore(LocalDate.now())

  def isAlmostExpired: Boolean = {
    val sixMonthsFromNow = LocalDate.now().plusMonths(6)
    expirationDate.isBefore(sixMonthsFromNow) && !isExpired
  }

  def isValid: Boolean =
    licenseId.nonEmpty &&
      driverId.nonEmpty &&
      licenseType.nonEmpty &&
      issueDate.isBefore(expirationDate)
}
