package ui

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import services.ServiceLocator

import javax.swing.table.AbstractTableModel

class LicenseTableModel(val columns: Array[String], val data: Array[Array[String]]) extends AbstractTableModel {

  def getRowCount: Int = data.length
  def getColumnCount: Int = columns.length
  def getValueAt(row: Int, col: Int): AnyRef = data(row)(col)
  override def getColumnName(col: Int): String = columns(col)
}

object LicenseTableModel {
  def modeloDefault(
                     services: ServiceLocator[IO],
                     xa: Transactor[IO]
                   ): IO[LicenseTableModel] = {
    getDBdata(services, xa).map { data =>
      val columnNames = Array("ID", "ID Conductor", "Tipo", "Estado", "Renovada", "Expedida", "Expira", "Restriciones")
      new LicenseTableModel(columnNames, data)
    }
  }



  def getDBdata(
                 services: ServiceLocator[IO],
                 xa: Transactor[IO]
               ): IO[Array[Array[String]]] = {
    val operations = for {
      licenses <- services.licenseService.findAll // Esto es ConnectionIO[List[license]]
    } yield licenses

    operations.transact(xa).map { licenses =>
      licenses.map { license =>
        Array(
          license.licenseId,
          license.driverId,
          license.licenseType,
          license.licenseStatus,
          if (license.renewed) "Sí" else "No",
          license.issueDate.toString,
          license.expirationDate.toString,
          license.restrictions.toString
        )
      }.toArray
    }
  }


}