import org.flywaydb.sbt.FlywayPlugin._
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys
import play.sbt.{PlaySettings, PlayScala}
import sbt._
import sbt.Keys._
import sbtbuildinfo.{BuildInfoPlugin, BuildInfoKey}
import sbtbuildinfo.BuildInfoKeys._
import spray.revolver.RevolverPlugin._

object SotaBuild extends Build {

  lazy val basicSettings = Seq(
    organization := "org.genivi",
    scalaVersion := "2.11.7",

    dependencyOverrides ++= Set(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
    )
  )

  lazy val commonSettings = basicSettings ++ Packaging.settings ++ Revolver.settings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value + ".sota." + name.value
  )

  lazy val externalResolver = Project(id = "resolver", base = file("external-resolver"),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Dependencies.Rest
    )
  ) enablePlugins (Packaging.plugins :+ BuildInfoPlugin :_*)


  lazy val core = Project(id = "core", base = file("core"),
    settings = commonSettings ++ Migrations.settings ++ Seq(
      libraryDependencies ++= Dependencies.Rest :+ Dependencies.NscalaTime,
      flywayUrl := "jdbc:mysql://localhost:3306/sota",
      flywayUser := "sota",
      flywayPassword := "s0ta"
    )
  ).enablePlugins(Packaging.plugins: _*)

  import play.sbt.Play.autoImport._
  lazy val webServer = Project(id = "webserver", base = file("web-server"),
    settings = commonSettings ++ PlaySettings.defaultScalaSettings ++ Seq(
      RoutesKeys.routesGenerator := InjectedRoutesGenerator,
      resolvers += "scalaz-bintray"  at "http://dl.bintray.com/scalaz/releases",
      libraryDependencies += specs2 % Test
    )).enablePlugins( PlayScala )

  lazy val sota = Project(id = "sota", base = file("."),
    settings = basicSettings ++ Versioning.settings
  )
    .aggregate(core, externalResolver, webServer)
    .enablePlugins(Versioning.Plugin)

}

object Dependencies {

  val AkkaHttpVersion = "1.0"

  val AkkaVersion = "2.3.12"

  lazy val Akka = Seq(
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % AkkaHttpVersion,
    "com.typesafe.akka" % "akka-http-experimental_2.11" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion
  )

  lazy val Scalaz = "org.scalaz" %% "scalaz-core" % "7.1.3"

  lazy val Slick = Seq(
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "com.zaxxer" % "HikariCP" % "2.3.8",
    "org.mariadb.jdbc" % "mariadb-java-client" % "1.2.0"
  )

  lazy val NscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.0.0"

  lazy val Rest = Akka ++ Slick

}
