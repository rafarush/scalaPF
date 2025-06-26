import cats.effect.{Deferred, ExitCode, IO, IOApp}
import config.AppConfig
import database.DatabaseConnection
import services._
import ui.MainWindow
import cats.effect.unsafe.implicits.global
import reports.PDFReportGenerator

import javax.swing.SwingUtilities
//import reports._
import doobie.implicits._
import models._
import pureconfig._
import pureconfig.generic.auto._
import java.time.LocalDate
import doobie.hi.connection

object Main extends IOApp {

//  def run(args: List[String]): IO[ExitCode] = {
//    for {
//      // Cargar configuración con manejo de errores
//      config <- IO.fromEither(ConfigSource.default.load[AppConfig])
//        .handleErrorWith(e => IO.raiseError(new RuntimeException(s"Failed to load config: $e")))
//
//      // Crear Deferred para esperar cierre ventana
//      deferred <- Deferred[IO, Unit]
//
//      exitCode <- DatabaseConnection.createTransactor[IO](config.database).use { xa =>
//        // Inicializar servicios con el transactor
//        val services = ServiceLocator[IO](xa) // Suponiendo que se inicializa con el transactor
//
//        IO {
//          SwingUtilities.invokeLater(() => {
//            val window = new MainWindow() // Pasar servicios y transactor al constructor
//            window.initUI(services, xa)
//            window.setVisible(true)
//            window.addWindowListener(new java.awt.event.WindowAdapter() {
//              override def windowClosing(e: java.awt.event.WindowEvent): Unit = {
//                deferred.complete(()).unsafeRunSync()
//              }
//            })
//          })
//        } *> deferred.get.as(ExitCode.Success) // Esperar cierre ventana y devolver éxito
//      }
//    } yield exitCode
//  }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      // Cargar configuración
      config <- IO.fromEither(
        ConfigSource.default.load[AppConfig].left.map(failures =>
          new RuntimeException(s"Failed to load config: $failures")
        )
      )

      // Crear Deferred para esperar cierre ventana
      deferred <- Deferred[IO, Unit]

      exitCode <- DatabaseConnection.createTransactor[IO](config.database).use { xa =>
        val pdfGen = new PDFReportGenerator[IO](xa)
        val entityName = "1" // <-- pon aquí el nombre que quieras buscar
        val outputPath = "reports"

        for {
          _ <- pdfGen.createRelatedEntityReportPDF(entityName, outputPath)
        } yield ExitCode.Success
      }


      exitCode <- DatabaseConnection.createTransactor[IO](config.database).use { xa =>
        // Inicializar servicios con el transactor
        val services = ServiceLocator[IO](xa) // Suponiendo que ServiceLocator se construye así

        IO {
          SwingUtilities.invokeLater(() => {
            val window = new MainWindow() // Pasar servicios y transactor al constructor
            window.initUI(services, xa)
            window.setVisible(true)
            window.addWindowListener(new java.awt.event.WindowAdapter() {
              override def windowClosing(e: java.awt.event.WindowEvent): Unit = {
                deferred.complete(()).unsafeRunSync()
              }
            })
          })
        } *> deferred.get.as(ExitCode.Success) // Esperar cierre ventana y devolver éxito
      }




    } yield exitCode
  }

//  def run(args: List[String]): IO[ExitCode] = {
//    for {
//      //Load configuration
//        config <- IO.fromEither(ConfigSource.default.load[AppConfig])
//        .handleErrorWith(e => IO.raiseError(new RuntimeException(s"Failed to load config: $e")))
//
//      // Create database transactor
//      _ <- DatabaseConnection.createTransactor[IO](config.database).use { xa =>
//        // Initialize services
//        val services = ServiceLocator[IO]
//      }
//
//      deferred <- Deferred[IO, Unit]
//      _ <- IO {
//        SwingUtilities.invokeLater(() => {
//          val window = new MainWindow()
//          window.initUI()
//          window.setVisible(true)
//          window.addWindowListener(new java.awt.event.WindowAdapter() {
//            override def windowClosing(e: java.awt.event.WindowEvent): Unit = {
//              deferred.complete(()).unsafeRunSync()
//            }
//          })
//        })
//      }
//      // Ejecutar el programa mientras la ventana está abierta
//      program <- {
//        val program = for {
//          config <- IO.fromEither(
//            ConfigSource.default.load[AppConfig].left.map(failures =>
//              new RuntimeException(failures.toList.map(_.description).mkString(", "))
//            )
//          ).handleErrorWith(e => IO.raiseError(new RuntimeException(s"Failed to load config: $e")))
//
//          _ <- DatabaseConnection.createTransactor[IO](config.database).use { xa =>
//            val services = ServiceLocator[IO]
//            for {
//              _ <- runSampleOperations(services, xa)
//            } yield ()
//          }
//        } yield ExitCode.Success
//
//        program.handleErrorWith { error =>
//          IO.println(s"Application failed with error: ${error.getMessage}") *>
//            IO.println(error.getStackTrace.mkString("\n")) *>
//            IO.pure(ExitCode.Error)
//        }
//      }.start // Ejecutar en paralelo
//
//      _ <- deferred.get // Esperar cierre ventana
//      exitCode <- program.joinWithNever // Esperar que termine el programa
//    } yield exitCode
//  }



  //  def run(args: List[String]): IO[ExitCode] = {
//    val program = for {
//      // Load configuration
//      config <- IO.fromEither(ConfigSource.default.load[AppConfig])
//        .handleErrorWith(e => IO.raiseError(new RuntimeException(s"Failed to load config: $e")))
//
//      // Create database transactor
//      _ <- DatabaseConnection.createTransactor[IO](config.database).use { xa =>
//        // Initialize services
//        val services = ServiceLocator[IO]
//        val htmlReportGen = HTMLReportGenerator[IO]
//        val pdfReportGen = PDFReportGenerator[IO]
//
//        for {
//          // Run sample operations
//          _ <- runSampleOperations(services, xa)
//
//          // Generate reports
//          _ <- generateReports(htmlReportGen, pdfReportGen, config.reports, xa)
//
//        } yield (())
//      }
//    } yield ExitCode.Success
//
//    program.handleErrorWith { error =>
//      IO.println(s"Application failed with error: ${error.getMessage}") *>
//        IO.println(error.getStackTrace.mkString("\n")) *>
//        IO.pure(ExitCode.Error)
//    }
//  }



  private def runSampleOperations(services: ServiceLocator[IO], xa: doobie.Transactor[IO]): IO[Unit] = {
    val operations = for {
//      driver <- services.driverService.create(Driver(
//        "12345678", "John", "Doe", LocalDate.of(1990, 1, 1),
//        "123 Main St", "555-1234", "john.doe@email.com"
//      ))
      driverById <- services.driverService.findById("20621205009")
      _ <- connection.delay(println(s"Driver by id (20621205009): $driverById"))

//      _ <- services.driverService.findById("12345678")

      driverCount <- services.driverService.count
      _ <- connection.delay(println(s"Total drivers: $driverCount"))

//      license <- services.licenseService.create(License(
//        "LIC001", "12345678", "A", LocalDate.now(), LocalDate.now().plusYears(5),
//        Some("None"), false, "Active"
//      ))
      licenseById <- services.licenseService.findById("20000678")
      _ <- connection.delay(println(s"License by id (20000678): $licenseById"))

      licenseCount <- services.licenseService.count
      _ <- connection.delay(println(s"Total licenses: $licenseCount"))

    } yield ()

    operations.transact(xa).handleErrorWith { error =>
      IO.println(s"Database operation failed: ${error.getMessage}")
    }
  }

//  private def generateReports(
//                               htmlGen: HTMLReportGenerator[IO],
//                               pdfGen: PDFReportGenerator[IO],
//                               reportsConfig: config.ReportsConfig,
//                               xa: doobie.Transactor[IO]
//                             ): IO[Unit] = {
//    val startDate = LocalDate.of(2020, 1, 1)
//    val endDate = LocalDate.of(2025, 12, 31)
//
//    val reportOperations = for {
//      _ <- htmlGen.createCenterReport(reportsConfig.htmlOutputPath)
//      _ <- htmlGen.createDriverReport("12345678", reportsConfig.htmlOutputPath)
//      _ <- htmlGen.createLicenseReport(startDate, endDate, reportsConfig.htmlOutputPath)
//      _ <- htmlGen.createInfractionReport(startDate, endDate, reportsConfig.htmlOutputPath)
//
//      _ <- pdfGen.createCenterReportPDF(reportsConfig.pdfOutputPath)
//      _ <- pdfGen.createDriverReportPDF("12345678", reportsConfig.pdfOutputPath)
//      _ <- pdfGen.createLicenseReportPDF(startDate, endDate, reportsConfig.pdfOutputPath)
//    } yield ()
//
//    reportOperations.transact(xa).void.handleErrorWith { error =>
//      IO.println(s"Report generation failed: ${error.getMessage}")
//    }
//  }


}
