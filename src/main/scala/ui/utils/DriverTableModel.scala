package ui

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import services.ServiceLocator

import javax.swing.table.AbstractTableModel

class DriverTableModel(val columns: Array[String], val data: Array[Array[String]]) extends AbstractTableModel {

  def getRowCount: Int = data.length
  def getColumnCount: Int = columns.length
  def getValueAt(row: Int, col: Int): AnyRef = data(row)(col)
  override def getColumnName(col: Int): String = columns(col)
}

object DriverTableModel {
  def modeloDefault(
                     services: ServiceLocator[IO],
                     xa: Transactor[IO]
                   ): IO[DriverTableModel] = {
    getDBdata(services, xa).map { data =>
      val columnNames = Array("Carne de Identidad", "Nombre", "Apellidos", "Fecha de Nacimiento", "Direccion", "Telefono", "Email")
      new DriverTableModel(columnNames, data)
    }
  }



  def getDBdata(
                 services: ServiceLocator[IO],
                 xa: Transactor[IO]
               ): IO[Array[Array[String]]] = {
    val operations = for {
      drivers <- services.driverService.findAll
    } yield drivers

    operations.transact(xa).map { drivers =>
      drivers.map { driver =>
        Array(
          driver.driverId,
          driver.firstName,
          driver.lastName,
          driver.birthDate.toString,
          driver.address,
          driver.phone,
          driver.email
        )
      }.toArray
    }
  }


}