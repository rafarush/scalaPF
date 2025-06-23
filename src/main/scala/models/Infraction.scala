package models

import java.time.LocalDate

case class Infraction(
                       infractionCode: String,
                       licenseId: String,
                       violationType: String,
                       date: LocalDate,
                       location: String,
                       description: String,
                       points: Int,
                       isPaid: Boolean
                     ) {
  def isRecent: Boolean = {
    val oneWeekAgo = LocalDate.now().minusWeeks(1)
    date.isAfter(oneWeekAgo)
  }

  def status: String = if (isPaid) "Paid" else "Pending"
}
