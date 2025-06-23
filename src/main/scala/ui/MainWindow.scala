package ui

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.util.transactor.Transactor
import services.ServiceLocator
import doobie.implicits._
import reports.PDFReportGenerator

import javax.swing.SwingUtilities
import javax.swing._
import java.awt._
import java.awt.event._
import java.io.File
import java.nio.file.Paths
import java.time.LocalDate

class MainWindow extends JFrame("Gestion de Licencias") {
  private val table = new JTable()
  private val swingMenuBar: JMenuBar = new JMenuBar()

  // menu entidades
  private val menuEntidades = new JMenu("Entidades")
  private val driverTableOption = new JMenuItem("Conductores")
  private val licenseTableOption = new JMenuItem("Licencias")

  // acciones
  private val menuAcciones = new JMenu("Acciones")
  private val createOption = new JMenuItem("Crear")
  private val deleteOption = new JMenuItem("Eliminar")
  private val updateOption = new JMenuItem("Modificar")

  // menu reportes
  private val menuReportes = new JMenu("Reportes")
  private val reporte1 = new JMenuItem("Ficha del Centro")
  private val reporte2 = new JMenuItem("Ficha del Conductor")
  private val reporte3 = new JMenuItem("Licencias Emitidas en los ultimos 10 años")
//  private val reporte4 = new JMenuItem("reporte4")
//  private val reporte5 = new JMenuItem("reporte5")
//  private val reporte6 = new JMenuItem("reporte6")
//  private val reporte7 = new JMenuItem("reporte7")
//  private val reporte8 = new JMenuItem("reporte8")
//  private val reporte9 = new JMenuItem("reporte9")


  def initUI(services: ServiceLocator[IO],
             xa: Transactor[IO]): Unit = {
    // Obtener las dimensiones de la pantalla
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    val screenWidth = screenSize.getWidth
    val screenHeight = screenSize.getHeight

    // Establecer el tamaño de la ventana como un porcentaje de la pantalla
    val windowWidth = (screenWidth * 0.8).toInt  // 80% del ancho de la pantalla
    val windowHeight = (screenHeight * 0.9).toInt // 80% del alto de la pantalla
    setSize(windowWidth, windowHeight)

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setLocationRelativeTo(null)


    val scrollPane = new JScrollPane(table)
    add(scrollPane, BorderLayout.CENTER)


    //MenuItems Functionalities

    //load drivers
    val loadDriverModel: IO[DriverTableModel] = DriverTableModel.modeloDefault(services, xa)
    loadDriverModel.unsafeRunAsync {
      case Right(model) =>
        SwingUtilities.invokeLater(() => {
          // Aquí asignas el modelo a tu JTable, por ejemplo:
          driverTableOption.addActionListener((_: ActionEvent) => table.setModel(model))
        })
      case Left(error) => IO.raiseError(new Exception("Error al cambiar modelo"))
    }

    //load licenses
    val loadLicenseModel: IO[LicenseTableModel] = LicenseTableModel.modeloDefault(services, xa)
    loadLicenseModel.unsafeRunAsync {
      case Right(model) =>
        SwingUtilities.invokeLater(() => {
          // Aquí asignas el modelo a tu JTable, por ejemplo:
          licenseTableOption.addActionListener((_: ActionEvent) => table.setModel(model))
        })
      case Left(error) => IO.raiseError(new Exception("Error al cambiar modelo"))
    }

    // delete driver
    deleteOption.addActionListener { (_: ActionEvent) =>
      val selectedRow = table.getSelectedRow
      if (selectedRow >= 0) {
        val driverId = table.getValueAt(selectedRow, 0).toString

        val ioDelete: IO[Either[String, Unit]] = services.driverService.delete(driverId).transact(xa)

        ioDelete.attempt.unsafeRunAsync {
          case Right(Right(_)) =>
            // Borrado exitoso, recargar modelo
            DriverTableModel.modeloDefault(services, xa).unsafeRunAsync {
              case Right(newModel) =>
                SwingUtilities.invokeLater(() => {
                  table.setModel(newModel)
                  JOptionPane.showMessageDialog(this, "Eliminado correctamente y tabla actualizada")
                })
              case Left(e) =>
                SwingUtilities.invokeLater(() => {
                  JOptionPane.showMessageDialog(this, s"Eliminado, pero error al actualizar tabla: ${e.getMessage}")
                })
            }
          case Right(Left(errorMsg)) =>
            SwingUtilities.invokeLater(() => {
              JOptionPane.showMessageDialog(this, s"Error al eliminar: $errorMsg")
            })
          case Left(e) =>
            SwingUtilities.invokeLater(() => {
              JOptionPane.showMessageDialog(this, s"Error inesperado: ${e.getMessage}")
            })
        }
      } else {
        JOptionPane.showMessageDialog(this, "Selecciona una fila para eliminar")
      }
    }

    // Reports
    val pdfGenerator = PDFReportGenerator[IO](xa)

    // Reporte Center
    reporte1.addActionListener { (_: ActionEvent) =>
      val currentWorkingDir = System.getProperty("user.dir")

      // Construye la ruta al directorio "reports"
      // Esto creará una ruta como "C:\Users\Rafa\Coding\IdeaProjects\TFSCALAsbt\reports"
      val outputPath = Paths.get(currentWorkingDir, "reports").toString

      // Asegúrate de que el directorio "reports" exista
      val reportsDir = new File(outputPath)
      if (!reportsDir.exists()) {
        reportsDir.mkdirs() // Crea el directorio y los padres si no existen
      }

      val pdfFile = Paths.get(s"$outputPath/centerReport.pdf").toFile

      // Ejecutar la generación en un hilo de IO
      pdfGenerator.createCenterReportPDF(outputPath).unsafeRunAsync {
        case Right(_) =>
          // Abrir el PDF en el hilo de Swing para evitar bloqueos
          SwingUtilities.invokeLater { () =>
            if (Desktop.isDesktopSupported && pdfFile.exists()) {
              try Desktop.getDesktop.open(pdfFile)
              catch {
                case ex: Exception =>
                  JOptionPane.showMessageDialog(null, s"No se pudo abrir el PDF: ${ex.getMessage}")
              }
            } else {
              JOptionPane.showMessageDialog(null, "No se pudo encontrar el archivo PDF generado.")
            }
          }
        case Left(error) =>
          SwingUtilities.invokeLater { () =>
            JOptionPane.showMessageDialog(null, s"Error al generar el PDF: ${error.getMessage}")
          }
      }
    }

    // Reporte Conductor
    reporte2.addActionListener { (_: ActionEvent) =>

      if (table.getModel.isInstanceOf[DriverTableModel]) {
        val selectedRow = table.getSelectedRow
        if (selectedRow >= 0) {
          val driverId = table.getValueAt(selectedRow, 0).toString
          val currentWorkingDir = System.getProperty("user.dir")

          // Construye la ruta al directorio "reports"
          // Esto creará una ruta como "C:\Users\Rafa\Coding\IdeaProjects\TFSCALAsbt\reports"
          val outputPath = Paths.get(currentWorkingDir, "reports").toString

          // Asegúrate de que el directorio "reports" exista
          val reportsDir = new File(outputPath)
          if (!reportsDir.exists()) {
            reportsDir.mkdirs() // Crea el directorio y los padres si no existen
          }

          val pdfFile = Paths.get(s"$outputPath/driverReport.pdf").toFile

          // Ejecutar la generación en un hilo de IO
          pdfGenerator.createDriverReportPDF(driverId, outputPath).unsafeRunAsync {
            case Right(_) =>
              // Abrir el PDF en el hilo de Swing para evitar bloqueos
              SwingUtilities.invokeLater { () =>
                if (Desktop.isDesktopSupported && pdfFile.exists()) {
                  try Desktop.getDesktop.open(pdfFile)
                  catch {
                    case ex: Exception =>
                      JOptionPane.showMessageDialog(null, s"No se pudo abrir el PDF: ${ex.getMessage}")
                  }
                } else {
                  JOptionPane.showMessageDialog(null, "No se pudo encontrar el archivo PDF generado.")
                }
              }
            case Left(error) =>
              SwingUtilities.invokeLater { () =>
                JOptionPane.showMessageDialog(null, s"Error al generar el PDF: ${error.getMessage}")
              }
          }
        }else{
          JOptionPane.showMessageDialog(null, "Debe seleccionar un conductor")
        }
      }else{
        JOptionPane.showMessageDialog(null, "Debe estar en la tabla conductor")
      }

    }

    // Reporte Licencias
    reporte3.addActionListener { (_: ActionEvent) =>
      val currentWorkingDir = System.getProperty("user.dir")

      // Construye la ruta al directorio "reports"
      // Esto creará una ruta como "C:\Users\Rafa\Coding\IdeaProjects\TFSCALAsbt\reports"
      val outputPath = Paths.get(currentWorkingDir, "reports").toString

      // Asegúrate de que el directorio "reports" exista
      val reportsDir = new File(outputPath)
      if (!reportsDir.exists()) {
        reportsDir.mkdirs() // Crea el directorio y los padres si no existen
      }

      val pdfFile = Paths.get(s"$outputPath/licenseReport.pdf").toFile

      // Ejecutar la generación en un hilo de IO
      pdfGenerator.createLicenseReportPDF(LocalDate.now().minusYears(10), LocalDate.now(), outputPath).unsafeRunAsync {
        case Right(_) =>
          // Abrir el PDF en el hilo de Swing para evitar bloqueos
          SwingUtilities.invokeLater { () =>
            if (Desktop.isDesktopSupported && pdfFile.exists()) {
              try Desktop.getDesktop.open(pdfFile)
              catch {
                case ex: Exception =>
                  JOptionPane.showMessageDialog(null, s"No se pudo abrir el PDF: ${ex.getMessage}")
              }
            } else {
              JOptionPane.showMessageDialog(null, "No se pudo encontrar el archivo PDF generado.")
            }
          }
        case Left(error) =>
          SwingUtilities.invokeLater { () =>
            JOptionPane.showMessageDialog(null, s"Error al generar el PDF: ${error.getMessage}")
          }
      }
    }


    // add items to menuEntidades
    menuEntidades.add(driverTableOption)
    menuEntidades.add(licenseTableOption)


    // add items to menuAcciones
    menuAcciones.add(createOption)
    menuAcciones.add(updateOption)
    menuAcciones.add(deleteOption)

    // add items to menuReportes
    menuReportes.add(reporte1)
    menuReportes.add(reporte2)
    menuReportes.add(reporte3)
//    menuReportes.add(reporte4)
//    menuReportes.add(reporte5)
//    menuReportes.add(reporte6)
//    menuReportes.add(reporte7)
//    menuReportes.add(reporte8)
//    menuReportes.add(reporte9)

    // add menus to menuBar
    swingMenuBar.add(menuEntidades)
    swingMenuBar.add(menuAcciones)
    swingMenuBar.add(menuReportes)

    setJMenuBar(swingMenuBar) // Aquí aseguramos que se usa correctamente JMenuBar
  }
}