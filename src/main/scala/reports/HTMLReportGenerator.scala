//package reports
//
//import cats.effect._
//import cats.implicits._
//import scalatags.Text.all._
//import models._
//import services._
//import doobie._
//import java.time.LocalDate
//import java.time.format.DateTimeFormatter
//import java.nio.file.{Files, Paths, StandardOpenOption}
//
//class HTMLReportGenerator[F[_]: Sync] {
//
//  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
//
//  def createCenterReport(outputPath: String): ConnectionIO[F[Unit]] = {
//    val centerService = CenterService[ConnectionIO]
//    centerService.findFirst.map { centerOpt =>
//      Sync[F].delay {
//        val htmlContent = centerOpt match {
//          case Some(center) => generateCenterHTML(center)
//          case None => generateNoCenterHTML()
//        }
//
//        val filePath = Paths.get(s"$outputPath/centerReport.html")
//        Files.write(filePath, htmlContent.getBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
//        println(s"Center report generated at: $filePath")
//      }
//    }
//  }
//
//  def createDriverReport(driverId: String, outputPath: String): ConnectionIO[F[Unit]] = {
//    val driverService = DriverService[ConnectionIO]
//    val licenseService = LicenseService[ConnectionIO]
//    val infractionService = InfractionService[ConnectionIO]
//
//    for {
//      driverOpt <- driverService.findById(driverId)
//      licenses <- licenseService.findAll.map(_.filter(_.driverId == driverId))
//      infractions <- infractionService.findAll.map(_.filter(inf =>
//        licenses.exists(_.licenseId == inf.licenseId)))
//    } yield {
//      Sync[F].delay {
//        val htmlContent = driverOpt match {
//          case Some(driver) => generateDriverHTML(driver, licenses, infractions)
//          case None => generateNoDriverHTML(driverId)
//        }
//
//        val filePath = Paths.get(s"$outputPath/driverReport.html")
//        Files.write(filePath, htmlContent.getBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
//        println(s"Driver report generated at: $filePath")
//      }
//    }
//  }
//
//  def createLicenseReport(startDate: LocalDate, endDate: LocalDate, outputPath: String): ConnectionIO[F[Unit]] = {
//    val licenseService = LicenseService[ConnectionIO]
//    val driverService = DriverService[ConnectionIO]
//
//    for {
//      licenses <- licenseService.findAll.map(_.filter(l =>
//        !l.issueDate.isBefore(startDate) && !l.issueDate.isAfter(endDate)))
//      drivers <- driverService.findAll
//    } yield {
//      Sync[F].delay {
//        val licensesWithDrivers = licenses.map { license =>
//          val driver = drivers.find(_.driverId == license.driverId)
//          (license, driver)
//        }
//
//        val htmlContent = generateLicenseReportHTML(licensesWithDrivers, startDate, endDate)
//        val filePath = Paths.get(s"$outputPath/licenseReport.html")
//        Files.write(filePath, htmlContent.getBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
//        println(s"License report generated at: $filePath")
//      }
//    }
//  }
//
//  def createInfractionReport(startDate: LocalDate, endDate: LocalDate, outputPath: String): ConnectionIO[F[Unit]] = {
//    val infractionService = InfractionService[ConnectionIO]
//    val licenseService = LicenseService[ConnectionIO]
//    val driverService = DriverService[ConnectionIO]
//
//    for {
//      infractions <- infractionService.findByDateRange(startDate, endDate)
//      licenses <- licenseService.findAll
//      drivers <- driverService.findAll
//    } yield {
//      Sync[F].delay {
//        val infractionsWithDrivers = infractions.map { infraction =>
//          val license = licenses.find(_.licenseId == infraction.licenseId)
//          val driver = license.flatMap(l => drivers.find(_.driverId == l.driverId))
//          (infraction, driver)
//        }
//
//        val htmlContent = generateInfractionReportHTML(infractionsWithDrivers, startDate, endDate)
//        val filePath = Paths.get(s"$outputPath/infractionReport.html")
//        Files.write(filePath, htmlContent.getBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
//        println(s"Infraction report generated at: $filePath")
//      }
//    }
//  }
//
//  private def generateCenterHTML(center: Center): String = {
//    val document = html(
//      head(
//        title("Center Report"),
//        tag("style")(
//          """
//          body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
//          h2 { text-align: center; color: #333; }
//          table { width: 100%; border-collapse: collapse; background-color: #fff; margin-top: 20px; }
//          th, td { border: 1px solid #ccc; padding: 8px; text-align: center; }
//          th { background-color: #007acc; color: white; }
//          tr:nth-child(even) { background-color: #f2f2f2; }
//          img { display: block; margin: 0 auto 20px auto; max-width: 200px; max-height: 100px; }
//          """
//        )
//      ),
//      body(
//        h2("Center Report"),
//        center.logo.map(logoPath => img(src := logoPath, alt := "Center Logo")),
//        table(
//          tr(
//            th("Center Name"), th("Postal Address"), th("Email"), th("Phone"),
//            th("General Director"), th("HR Manager"), th("Accounting Manager"), th("Secretary")
//          ),
//          tr(
//            td(center.centerName), td(center.postalAddress), td(center.centerEmail), td(center.phone),
//            td(center.generalDirectorName), td(center.hrManager), td(center.accountingManager), td(center.secretaryName)
//          )
//        )
//      )
//    )
//    "<!DOCTYPE html>\n" + document.render
//  }
//
//  private def generateNoCenterHTML(): String = {
//    val document = html(
//      head(title("Center Report")),
//      body(
//        h2("Center Report"),
//        p("No center found in the database.")
//      )
//    )
//    "<!DOCTYPE html>\n" + document.render
//  }
//
//  private def generateDriverHTML(driver: Driver, licenses: List[License], infractions: List[Infraction]): String = {
//    val document = html(
//      head(
//        title("Driver Report"),
//        tag("style")(
//          """
//          body { font-family: Arial, sans-serif; background: #f9f9f9; padding: 20px; }
//          h2, h3 { color: #004080; }
//          table { border-collapse: collapse; width: 100%; background: white; margin-bottom: 20px; }
//          th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
//          th { background-color: #007acc; color: white; }
//          tr:nth-child(even) { background-color: #f2f2f2; }
//          """
//        )
//      ),
//      body(
//        h2("Driver Report"),
//        h3("Driver Information"),
//        table(
//          tr(th("Name"), td(driver.fullName)),
//          tr(th("Birth Date"), td(driver.birthDate.format(dateFormatter))),
//          tr(th("Address"), td(driver.address)),
//          tr(th("Phone"), td(driver.phone)),
//          tr(th("Email"), td(driver.email))
//        ),
//        h3("Issued Licenses"),
//        table(
//          tr(th("Type"), th("Issue Date"), th("Expiration Date"), th("Restrictions"), th("Renewed"), th("Status")),
//          licenses.map { license =>
//            tr(
//              td(license.licenseType),
//              td(license.issueDate.format(dateFormatter)),
//              td(license.expirationDate.format(dateFormatter)),
//              td(license.restrictions.getOrElse("")),
//              td(if (license.renewed) "Yes" else "No"),
//              td(license.licenseStatus)
//            )
//          }
//        ),
//        h3("Registered Infractions"),
//        table(
//          tr(th("Violation Type"), th("Date"), th("Points")),
//          infractions.map { infraction =>
//            tr(
//              td(infraction.violationType),
//              td(infraction.date.format(dateFormatter)),
//              td(infraction.points.toString)
//            )
//          }
//        )
//      )
//    )
//    "<!DOCTYPE html>\n" + document.render
//  }
//
//  private def generateNoDriverHTML(driverId: String): String = {
//    val document = html(
//      head(title("Driver Report")),
//      body(
//        h2("Driver Report"),
//        p(s"No driver found with ID: $driverId")
//      )
//    )
//    "<!DOCTYPE html>\n" + document.render
//  }
//
//  private def generateLicenseReportHTML(licensesWithDrivers: List[(License, Option[Driver])], startDate: LocalDate, endDate: LocalDate): String = {
//    val document = html(
//      head(
//        title("Issued Licenses Report"),
//        tag("style")(
//          """
//          body { font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px; }
//          h2 { text-align: center; color: #004080; }
//          table { border-collapse: collapse; width: 100%; background-color: white; }
//          th, td { border: 1px solid #ccc; padding: 8px; text-align: center; }
//          th { background-color: #007acc; color: white; }
//          tr:nth-child(even) { background-color: #f2f2f2; }
//          """
//        )
//      ),
//      body(
//        h2("Issued Licenses Report"),
//        p(s"From: $startDate To: $endDate"),
//        table(
//          tr(th("License Code"), th("Driver Name"), th("License Type"), th("Issue Date"), th("Expiration Date"), th("Status")),
//          licensesWithDrivers.map { case (license, driverOpt) =>
//            tr(
//              td(license.licenseId),
//              td(driverOpt.map(_.fullName).getOrElse("Unknown")),
//              td(license.licenseType),
//              td(license.issueDate.format(dateFormatter)),
//              td(license.expirationDate.format(dateFormatter)),
//              td(license.licenseStatus)
//            )
//          }
//        )
//      )
//    )
//    "<!DOCTYPE html>\n" + document.render
//  }
//
//  private def generateInfractionReportHTML(infractionsWithDrivers: List[(Infraction, Option[Driver])], startDate: LocalDate, endDate: LocalDate): String = {
//    val document = html(
//      head(
//        title("Registered Infractions Report"),
//        tag("style")(
//          """
//          body { font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px; }
//          h2 { text-align: center; color: #004080; }
//          table { border-collapse: collapse; width: 100%; background-color: white; }
//          th, td { border: 1px solid #ccc; padding: 8px; text-align: center; }
//          th { background-color: #007acc; color: white; }
//          tr:nth-child(even) { background-color: #f2f2f2; }
//          """
//        )
//      ),
//      body(
//        h2("Registered Infractions Report"),
//        p(s"From: $startDate To: $endDate"),
//        table(
//          tr(th("Infraction Code"), th("Driver Name"), th("Violation Type"), th("Date"), th("Location"), th("Points"), th("Status")),
//          infractionsWithDrivers.map { case (infraction, driverOpt) =>
//            tr(
//              td(infraction.infractionCode),
//              td(driverOpt.map(_.fullName).getOrElse("Unknown")),
//              td(infraction.violationType),
//              td(infraction.date.format(dateFormatter)),
//              td(infraction.location),
//              td(infraction.points.toString),
//              td(infraction.status)
//            )
//          }
//        )
//      )
//    )
//    "<!DOCTYPE html>\n" + document.render
//  }
//}
//
//object HTMLReportGenerator {
//  def apply[F[_]: Sync]: HTMLReportGenerator[F] = new HTMLReportGenerator[F]
//}
