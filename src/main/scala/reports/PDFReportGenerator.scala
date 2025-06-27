package reports

import cats.effect._
import cats.implicits._
import com.itextpdf.text._
import com.itextpdf.text.pdf._
import models._
import services._
import doobie._
import doobie.implicits._
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.FileOutputStream
import java.nio.file.Paths

class PDFReportGenerator[F[_]: Sync](xa: Transactor[F]) {

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")


  def createRelatedEntityReportPDF(entityName: String, outputPath: String): F[Unit] = {
    val relatedEntityService = ServiceLocator.apply(xa).relatedEntityService

    val action: ConnectionIO[Option[RelatedEntity]] = relatedEntityService.findByName(entityName)

    // Ejecutar la consulta y obtener resultado en F
    val program: F[Option[RelatedEntity]] = action.transact(xa)

    // Luego, en F, hacer el match y los efectos secundarios
    program.flatMap {
      case Some(relatedEntity) => Sync[F].delay(generateRelatedEnityReportPDF(relatedEntity, outputPath))
      case None => Sync[F].delay(println("No related entity found in database"))
    }
  }

  def generateRelatedEnityReportPDF(relatedEntity: RelatedEntity, outputPath: String): Unit = {
    val document = new Document()
    val pdfPath = java.nio.file.Paths.get(outputPath, "relatedEntityReporte.pdf").toString
    PdfWriter.getInstance(document, new FileOutputStream(pdfPath))
    document.open()

    val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
    val labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE)
    val valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK)

    val title = new Paragraph("Ficha de Entidad Asociada", titleFont)
    title.setAlignment(Element.ALIGN_CENTER)
    document.add(title)
    document.add(new Paragraph(" "))

    val table = new PdfPTable(2)
    table.setWidthPercentage(100)
    table.setSpacingBefore(10f)
    table.setSpacingAfter(10f)

    def addRow(label: String, value: String): Unit = {
      val labelCell = new PdfPCell(new Phrase(label, labelFont))
      labelCell.setBackgroundColor(new BaseColor(0, 122, 204))
      labelCell.setPadding(6)
      labelCell.setHorizontalAlignment(Element.ALIGN_LEFT)
      labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE)

      val valueCell = new PdfPCell(new Phrase(Option(value).getOrElse(""), valueFont))
      valueCell.setPadding(6)
      valueCell.setHorizontalAlignment(Element.ALIGN_LEFT)

      table.addCell(labelCell)
      table.addCell(valueCell)
    }

    addRow("Nombre", relatedEntity.entityName)
    addRow("Tipo", relatedEntity.entityType)
    addRow("Dirección", relatedEntity.address)
    addRow("Teléfono", relatedEntity.phone)
    addRow("Email", relatedEntity.contactEmail)
    addRow("Director", relatedEntity.directorName)
    addRow("Código del Centro", relatedEntity.centerCode)

    document.add(table)
    document.close()
    println(s"Reporte PDF generado correctamente en: $pdfPath")
  }






  def createCenterReportPDF(outputPath: String): F[Unit] = {
    val centerService = CenterService[F](xa)

    val action: ConnectionIO[Option[Center]] = centerService.findFirst

    // Ejecutar la consulta y obtener resultado en F
    val program: F[Option[Center]] = action.transact(xa)

    // Luego, en F, hacer el match y los efectos secundarios
    program.flatMap {
      case Some(center) => Sync[F].delay(generateCenterPDF(center, outputPath))
      case None => Sync[F].delay(println("No center found in database"))
    }
  }

  def createDriverReportPDF(driverId: String, outputPath: String): F[Unit] = {
    val driverService = DriverService[F](xa)
    val licenseService = LicenseService[F](xa)
    val infractionService = InfractionService[F](xa)

    val action: ConnectionIO[(Option[Driver], scala.List[License], scala.List[Infraction])] = for {
      driverOpt <- driverService.findById(driverId)
      licenses <- licenseService.findAll.map(_.filter(_.driverId == driverId))
      infractions <- infractionService.findAll.map(_.filter(inf =>
        licenses.exists(_.licenseId == inf.licenseId)))
    } yield (driverOpt, licenses, infractions)

    action.transact(xa).flatMap {
      case (Some(driver), licenses, infractions) =>
        Sync[F].delay(generateDriverPDF(driver, licenses, infractions, outputPath))
      case (None, _, _) =>
        Sync[F].delay(println(s"No driver found with ID: $driverId"))
    }
  }

  def createLicenseReportPDF(startDate: LocalDate, endDate: LocalDate, outputPath: String): F[Unit] = {
    val licenseService = LicenseService[F](xa)
    val driverService = DriverService[F](xa)

    val action: ConnectionIO[(scala.List[License], scala.List[Driver])] = for {
      licenses <- licenseService.findAll.map(_.filter(l =>
        !l.issueDate.isBefore(startDate) && !l.issueDate.isAfter(endDate)))
      drivers <- driverService.findAll
    } yield (licenses, drivers)

    action.transact(xa).flatMap { case (licenses, drivers) =>
      val licensesWithDrivers = licenses.map { license =>
        val driverOpt = drivers.find(_.driverId == license.driverId)
        (license, driverOpt)
      }
      Sync[F].delay(generateLicenseReportPDF(licensesWithDrivers, startDate, endDate, outputPath))
    }
  }

  private def generateCenterPDF(center: Center, outputPath: String): Unit = {
    val document = new Document()
    val filePath = Paths.get(s"$outputPath/centerReport.pdf")

    try {
      PdfWriter.getInstance(document, new FileOutputStream(filePath.toString))
      document.open()

      // Title
      val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK)
      val title = new Paragraph("Center Report", titleFont)
      title.setAlignment(Element.ALIGN_CENTER)
      document.add(title)
      document.add(Chunk.NEWLINE)

      // Table
      val table = new PdfPTable(8)
      table.setWidthPercentage(100)

      val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE)
      val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK)

      // Headers
      val headers = scala.List("Center Name", "Postal Address", "Email", "Phone",
        "General Director", "HR Manager", "Accounting Manager", "Secretary")

      headers.foreach { header =>
        val cell = new PdfPCell(new Phrase(header, headerFont))
        cell.setBackgroundColor(new BaseColor(0, 122, 204))
        cell.setHorizontalAlignment(Element.ALIGN_CENTER)
        cell.setPadding(8)
        table.addCell(cell)
      }

      // Data
      val data = scala.List(center.centerName, center.postalAddress, center.centerEmail, center.phone,
        center.generalDirectorName, center.hrManager, center.accountingManager, center.secretaryName)

      data.foreach { value =>
        val cell = new PdfPCell(new Phrase(value, cellFont))
        cell.setHorizontalAlignment(Element.ALIGN_CENTER)
        cell.setPadding(6)
        table.addCell(cell)
      }

      document.add(table)
      println(s"PDF report generated at: $filePath")

    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      document.close()
    }
  }

  private def generateDriverPDF(driver: Driver, licenses: scala.List[License], infractions: scala.List[Infraction], outputPath: String): Unit = {
    val document = new Document()
    val filePath = Paths.get(s"$outputPath/driverReport.pdf")

    try {
      PdfWriter.getInstance(document, new FileOutputStream(filePath.toString))
      document.open()

      val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK)
      val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE)
      val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK)

      // Title
      val title = new Paragraph("Driver Report", titleFont)
      title.setAlignment(Element.ALIGN_CENTER)
      document.add(title)
      document.add(new Paragraph("\n"))

      // Driver info
      val driverInfoTitle = new Paragraph("Driver Information", headerFont)
      document.add(driverInfoTitle)

      val driverTable = new PdfPTable(2)
      driverTable.setWidthPercentage(100)
      driverTable.setSpacingBefore(10)
      driverTable.setSpacingAfter(20)

      val driverData = scala.List(
        ("Name", driver.fullName),
        ("Birth Date", driver.birthDate.format(dateFormatter)),
        ("Address", driver.address),
        ("Phone", driver.phone),
        ("Email", driver.email)
      )

      driverData.foreach { case (label, value) =>
        val labelCell = new PdfPCell(new Phrase(label, headerFont))
        labelCell.setBackgroundColor(new BaseColor(0, 122, 204))
        labelCell.setPadding(6)
        driverTable.addCell(labelCell)

        val valueCell = new PdfPCell(new Phrase(value, cellFont))
        valueCell.setPadding(6)
        driverTable.addCell(valueCell)
      }

      document.add(driverTable)
      println(s"Driver PDF report generated at: $filePath")

    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      document.close()
    }
  }

  private def generateLicenseReportPDF(licensesWithDrivers: scala.List[(License, Option[Driver])],
                                       startDate: LocalDate, endDate: LocalDate, outputPath: String): Unit = {
    val document = new Document()
    val filePath = Paths.get(s"$outputPath/licenseReport.pdf")

    try {
      PdfWriter.getInstance(document, new FileOutputStream(filePath.toString))
      document.open()

      val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
      val subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12)
      val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE)
      val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK)

      val title = new Paragraph("Issued Licenses Report", titleFont)
      title.setAlignment(Element.ALIGN_CENTER)
      document.add(title)

      document.add(new Paragraph(s"From: $startDate  To: $endDate", subtitleFont))
      document.add(Chunk.NEWLINE)

      val table = new PdfPTable(6)
      table.setWidthPercentage(100)
      table.setSpacingBefore(10f)
      table.setSpacingAfter(10f)

      val headers = scala.List("License Code", "Driver Name", "License Type", "Issue Date", "Expiration Date", "Status")
      headers.foreach { header =>
        val headerCell = new PdfPCell(new Phrase(header, headerFont))
        headerCell.setBackgroundColor(new BaseColor(0, 122, 204))
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER)
        headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE)
        headerCell.setPadding(6)
        table.addCell(headerCell)
      }

      licensesWithDrivers.foreach { case (license, driverOpt) =>
        table.addCell(new Phrase(license.licenseId, cellFont))
        table.addCell(new Phrase(driverOpt.map(_.fullName).getOrElse("Unknown"), cellFont))
        table.addCell(new Phrase(license.licenseType, cellFont))
        table.addCell(new Phrase(license.issueDate.format(dateFormatter), cellFont))
        table.addCell(new Phrase(license.expirationDate.format(dateFormatter), cellFont))
        table.addCell(new Phrase(license.licenseStatus, cellFont))
      }

      document.add(table)
      println(s"License PDF report generated at: $filePath")

    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      document.close()
    }

  }
}

object PDFReportGenerator {
  def apply[F[_]: Sync](xa: Transactor[F]): PDFReportGenerator[F] = new PDFReportGenerator[F](xa)
}
