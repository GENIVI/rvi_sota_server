import com.typesafe.sbt.packager.docker.DockerPlugin
import org.flywaydb.sbt.FlywayPlugin._
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys
import play.sbt.{PlaySettings, PlayScala}
import sbt._
import sbt.Keys._
import sbtbuildinfo.{BuildInfoPlugin, BuildInfoKey}
import sbtbuildinfo.BuildInfoKeys._
import spray.revolver.RevolverPlugin._
import com.typesafe.sbt.packager.Keys.dockerExposedPorts

object SotaBuild extends Build {

  lazy val basicSettings = Seq(
    organization := "org.genivi",
    scalaVersion := "2.11.7",

    libraryDependencies ++= Dependencies.TestFrameworks, 

    dependencyOverrides ++= Set(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
      "com.google.guava"  % "guava" % "18.0"
    )
  )

  lazy val commonSettings = basicSettings ++ Packaging.settings ++ Revolver.settings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value + ".sota." + name.value
  )

  lazy val externalResolver = Project(id = "resolver", base = file("external-resolver"))
    .settings( commonSettings ++ Seq(
      libraryDependencies ++= Dependencies.Rest :+ Dependencies.Scalaz,
      dockerExposedPorts := Seq(8081)
    ))
    .enablePlugins(Packaging.plugins :+ BuildInfoPlugin :_*)


  lazy val core = Project(id = "core", base = file("core"))
    .settings( commonSettings ++ Migrations.settings ++ Seq(
      libraryDependencies ++= Dependencies.Rest :+ Dependencies.NscalaTime,
      dockerExposedPorts := Seq(8080),
      flywayUrl := sys.env.get("CORE_DB_URL").orElse( sys.props.get("core.db.url") ).getOrElse("jdbc:mysql://localhost:3306/sota"),
      flywayUser := sys.env.get("CORE_DB_USER").orElse( sys.props.get("core.db.user") ).getOrElse("sota"),
      flywayPassword := sys.env.get("CORE_DB_PASSWORD").orElse( sys.props.get("core.db.password")).getOrElse("s0ta")
    ))
    .enablePlugins(Packaging.plugins: _*)

  import play.sbt.Play.autoImport._
  lazy val webServer = Project(id = "webserver", base = file("web-server"),
    settings = commonSettings ++ PlaySettings.defaultScalaSettings ++ Seq(
      RoutesKeys.routesGenerator := InjectedRoutesGenerator,
      resolvers += "scalaz-bintray"  at "http://dl.bintray.com/scalaz/releases",
      dockerExposedPorts := Seq(9000),
      libraryDependencies ++= Seq (
        "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
        specs2 % Test,
        ws
      )
    )).enablePlugins( PlayScala )

  lazy val sota = Project(id = "sota", base = file("."))
    .settings( basicSettings )
    .settings( Versioning.settings )
    .settings( Release.settings )
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
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % AkkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.0.13"
  )

  lazy val Scalaz = "org.scalaz" %% "scalaz-core" % "7.1.3"

  lazy val ScalaTest = "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

  lazy val ScalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.4" % "test"

  lazy val TestFrameworks = Seq( ScalaTest, ScalaCheck )

  lazy val Slick = Seq(
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "com.zaxxer" % "HikariCP" % "2.3.8",
    "org.mariadb.jdbc" % "mariadb-java-client" % "1.2.0"
  )

  lazy val NscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.0.0"

  lazy val Rest = Akka ++ Slick

}
