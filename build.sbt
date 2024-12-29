val scala3Version = "3.6.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scyllaproof",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-cassandra" % "4.8.0",
      "org.typelevel" %% "cats-effect" % "3.5.1"
    )
  )
