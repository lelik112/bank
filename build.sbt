organization in ThisBuild := "net.tcheltsov"
version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.8"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"

lazy val `application` = (project in file ("."))
  .aggregate(`person-api`, `person-impl`, `card-api`, `card-impl`, `payment-api`, `payment-impl`)

lazy val `person-api` = (project in file ("person-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
lazy val `person-impl` = (project in file ("person-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      macwire
    )
  )
  .dependsOn(`person-api`, `card-api`)

lazy val `card-api` = (project in file ("card-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `card-impl` = (project in file ("card-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      macwire
    )
  )
  .dependsOn(`card-api`, `payment-api`)

lazy val `payment-api` = (project in file ("payment-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `payment-impl` = (project in file ("payment-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      macwire
    )
  )
  .dependsOn(`payment-api`)