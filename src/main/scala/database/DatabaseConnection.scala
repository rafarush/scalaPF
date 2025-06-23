package database

import cats.effect._
import doobie._
import doobie.hikari.HikariTransactor
import config.DatabaseConfig

object DatabaseConnection {

  def createTransactor[F[_]: Async](config: DatabaseConfig): Resource[F, HikariTransactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](config.maxConnections)
      xa <- HikariTransactor.newHikariTransactor[F](
        config.driver,
        config.url,
        config.user,
        config.password,
        ce
      )
    } yield xa
  }
}
