package models

import java.time.LocalDate

case class Test(
                 testCode: String,
                 testType: String,
                 date: LocalDate,
                 result: Boolean,
                 entityName: String,
                 examinerName: String,
                 driverId: String,
                 licenseType: String
               ) {
  def passed: Boolean = result
  def failed: Boolean = !result

  def isRecent: Boolean = {
    val sixMonthsAgo = LocalDate.now().minusMonths(6)
    date.isAfter(sixMonthsAgo)
  }
}
