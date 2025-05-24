ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "demo",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.17.0",
      "org.typelevel" %% "cats-core" % "2.10.0",
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "redis.clients" % "jedis" % "5.0.2",
      "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2"
    )
  )
