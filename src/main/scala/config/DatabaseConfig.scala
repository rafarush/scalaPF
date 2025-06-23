package config

import pureconfig._
import pureconfig.generic.auto._

case class DatabaseConfig(
                           driver: String,
                           url: String,
                           user: String,
                           password: String,
                           maxConnections: Int
                         )

case class AppConfig(
                      database: DatabaseConfig,
                      reports: ReportsConfig
                    )

case class ReportsConfig(
                          htmlOutputPath: String,
                          pdfOutputPath: String
                        )
