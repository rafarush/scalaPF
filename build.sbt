ThisBuild / version := "1.0.0"
ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "driving-license-management",
    libraryDependencies ++= Seq(
      // Cats Effect for functional programming
      "org.typelevel" %% "cats-effect" % "3.5.2",
      "org.typelevel" %% "cats-core" % "2.10.0",

      // Database access
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4",

      // Configuration
      "com.github.pureconfig" %% "pureconfig" % "0.17.4",
      "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.4",

      // JSON handling
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",

      // HTML generation
      "com.lihaoyi" %% "scalatags" % "0.12.0",

      // PDF generation
      "com.itextpdf" % "itextpdf" % "5.5.13.3",

      // Logging
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test,
      "org.tpolecat" %% "doobie-scalatest" % "1.0.0-RC4" % Test,

      // Visual
      "com.formdev" % "flatlaf" % "3.4"
    ),

    // Compiler options
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xlint"
    )
  )
