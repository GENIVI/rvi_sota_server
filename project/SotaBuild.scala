import com.typesafe.sbt.packager.docker.DockerPlugin
import org.flywaydb.sbt.FlywayPlugin._
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys
import play.sbt.{PlaySettings, PlayScala}
import sbt._
import sbt.Keys._
import sbtbuildinfo.{BuildInfoPlugin, BuildInfoKey}
import sbtbuildinfo.BuildInfoKeys._
import com.typesafe.sbt.packager.Keys.dockerExposedPorts
import com.typesafe.sbt.web._


object SotaBuild extends Build {

  lazy val UnitTests = config("ut") extend Test

  lazy val IntegrationTests = config("it") extend Test

  lazy val BrowserTests = config("bt") extend Test

  lazy val basicSettings = Seq(
    organization := "org.genivi",
    scalaVersion := "2.11.7",
    publishArtifact in Test := false,
    resolvers += Resolver.sonatypeRepo("snapshots"),

    libraryDependencies ++= Dependencies.TestFrameworks,

    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
    ),

    testFrameworks := Seq(sbt.TestFrameworks.ScalaTest),

    dependencyOverrides ++= Set(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
      "com.google.guava"  % "guava" % "18.0"
    ),

    shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }
  )


  lazy val compilerSettings = Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint", "-language:higherKinds"),
    javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation")
  )

  lazy val commonSettings = basicSettings ++ compilerSettings ++ Packaging.settings ++ Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value + ".sota." + name.value
  )

  lazy val mavenUser = sys.env.get("MAVEN_USER").orElse(sys.props.get("maven.user")).getOrElse("releaser")
  lazy val mavenPassword = sys.env.get("MAVEN_PASSWORD").orElse(sys.props.get("maven.password")).getOrElse("")

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    credentials += Credentials("Repository Archiva Managed internal Repository",
      "maven.advancedtelematic.com",
      mavenUser, mavenPassword),
    credentials += Credentials("Repository Archiva Managed snapshots Repository",
      "maven.advancedtelematic.com",
      mavenUser, mavenPassword),
    publishTo := {
      val server = "http://maven.advancedtelematic.com/repository/"
      if (isSnapshot.value)
        Some("snapshots" at server + "snapshots")
      else
        Some("internal"  at server + "internal")
    }
  )

  lazy val disablePublishSettings = Seq(
    publish := (),
    publishLocal := ()
  )

  // the sub-projects
  lazy val common = Project(id = "common", base = file("common"))
    .settings(basicSettings ++ compilerSettings)
    .settings( libraryDependencies ++= Dependencies.Rest ++ Dependencies.Circe :+ Dependencies.NscalaTime :+ Dependencies.Refined :+ Dependencies.CommonsCodec)
    .dependsOn(commonData)
    .settings(disablePublishSettings)

  lazy val commonData = Project(id = "common-data", base = file("common-data"))
    .settings(basicSettings ++ compilerSettings)
    .settings(libraryDependencies ++= Dependencies.Circe :+ Dependencies.Cats :+ Dependencies.Refined :+ Dependencies.CommonsCodec)
    .settings(publishSettings)

  lazy val commonTest = Project(id = "common-test", base = file("common-test"))
    .settings(basicSettings ++ compilerSettings)
    .settings(libraryDependencies ++= Seq (Dependencies.Cats, Dependencies.Refined))
    .dependsOn(commonData)
    .settings(disablePublishSettings)

  lazy val externalResolver = Project(id = "resolver", base = file("external-resolver"))
    .settings( commonSettings ++ Migrations.settings ++ Seq(
      libraryDependencies ++= Dependencies.Rest ++ Dependencies.Circe :+ Dependencies.Cats :+ Dependencies.Refined :+ Dependencies.ParserCombinators :+ Dependencies.Flyway,
      parallelExecution in Test := false,
      dockerExposedPorts := Seq(8081),
      flywayUrl := sys.env.get("RESOLVER_DB_URL").orElse( sys.props.get("resolver.db.url") ).getOrElse("jdbc:mysql://localhost:3306/sota_resolver"),
      flywayUser := sys.env.get("RESOLVER_DB_USER").orElse( sys.props.get("resolver.db.user") ).getOrElse("sota"),
      flywayPassword := sys.env.get("RESOLVER_DB_PASSWORD").orElse( sys.props.get("resolver.db.password")).getOrElse("s0ta")
    ))
    .dependsOn(common, commonData, commonTest % "test")
    .enablePlugins(Packaging.plugins :+ BuildInfoPlugin :_*)
    .settings(disablePublishSettings)

  lazy val core = Project(id = "core", base = file("core"))
    .settings( commonSettings ++ Migrations.settings ++ Seq(
      libraryDependencies ++= Dependencies.Rest ++ Dependencies.Circe :+ Dependencies.NscalaTime :+ Dependencies.Scalaz :+ Dependencies.Flyway,
      testOptions in UnitTests += Tests.Argument(TestFrameworks.ScalaTest, "-l", "RequiresRvi"),
      testOptions in IntegrationTests += Tests.Argument(TestFrameworks.ScalaTest, "-n", "RequiresRvi"),
      parallelExecution in Test := false,
      dockerExposedPorts := Seq(8080),
      flywayUrl := sys.env.get("CORE_DB_URL").orElse( sys.props.get("core.db.url") ).getOrElse("jdbc:mysql://localhost:3306/sota_core"),
      flywayUser := sys.env.get("CORE_DB_USER").orElse( sys.props.get("core.db.user") ).getOrElse("sota"),
      flywayPassword := sys.env.get("CORE_DB_PASSWORD").orElse( sys.props.get("core.db.password")).getOrElse("s0ta")
    ))
    .settings(inConfig(UnitTests)(Defaults.testTasks): _*)
    .settings(inConfig(IntegrationTests)(Defaults.testTasks): _*)
    .configs(IntegrationTests, UnitTests)
    .dependsOn(common, commonData, commonTest % "test")
    .enablePlugins(Packaging.plugins: _*)
    .settings(disablePublishSettings)

  import play.sbt.Play.autoImport._
  lazy val webServer = Project(id = "webserver", base = file("web-server"),
    settings = commonSettings ++ PlaySettings.defaultScalaSettings ++ Seq(
      RoutesKeys.routesGenerator := InjectedRoutesGenerator,
      testOptions in UnitTests += Tests.Argument(TestFrameworks.ScalaTest, "-l", "APITests BrowserTests"),
      testOptions in IntegrationTests += Tests.Argument(TestFrameworks.ScalaTest, "-n", "APITests"),
      testOptions in BrowserTests += Tests.Argument(TestFrameworks.ScalaTest, "-n", "BrowserTests"),
      resolvers += "scalaz-bintray"  at "http://dl.bintray.com/scalaz/releases",
      dockerExposedPorts := Seq(9000),
      libraryDependencies ++= Seq (
        "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
        "org.webjars" %% "webjars-play" % "2.4.0-1",
        "org.webjars" % "webjars-locator" % "0.27",
        "org.webjars.bower" % "react" % "0.13.3",
        "org.webjars.bower" % "react-router" % "0.13.3",
        "org.webjars.bower" % "flux" % "2.0.2",
        "org.webjars.bower" % "backbone" % "1.2.1",
        "org.webjars" % "bootstrap" % "3.3.4",
        "jp.t2v" %% "play2-auth"        % "0.14.0",
        "jp.t2v" %% "play2-auth-test"   % "0.14.0" % "test",
        "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
        "org.mindrot" % "jbcrypt" % "0.3m",
        ws,
        play.sbt.Play.autoImport.cache
      ) ++ Dependencies.Database
    ))
    .dependsOn(common)
    .enablePlugins(PlayScala, SbtWeb)
    .settings(inConfig(UnitTests)(Defaults.testTasks): _*)
    .settings(inConfig(IntegrationTests)(Defaults.testTasks): _*)
    .settings(inConfig(BrowserTests)(Defaults.testTasks): _*)
    .configs(UnitTests, IntegrationTests, BrowserTests)
    .settings(disablePublishSettings)

  lazy val sota = Project(id = "sota", base = file("."))
    .settings( basicSettings )
    .settings( Versioning.settings )
    .settings( Release.settings )
    .aggregate(common, commonData, commonTest, core, externalResolver, webServer)
    .enablePlugins(Versioning.Plugin)
    .settings(disablePublishSettings)

}

object Dependencies {

  val AkkaHttpVersion = "1.0"

  val AkkaVersion = "2.4.0"

  val CirceVersion = "0.2.0"


  lazy val Akka = Seq(
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % AkkaHttpVersion,
    "com.typesafe.akka" % "akka-http-experimental_2.11" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.0.13"
  )

  lazy val Circe = Seq(
    "io.circe" %% "circe-core"    % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parse"   % CirceVersion
  )

  lazy val Refined = "eu.timepit" %% "refined" % "0.3.1"

  lazy val Scalaz = "org.scalaz" %% "scalaz-core" % "7.1.3"
  lazy val Cats   = "org.spire-math" %% "cats" % "0.3.0"

  lazy val ScalaTest = "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

  lazy val ScalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.4" % "test"

  lazy val Flyway = "org.flywaydb" % "flyway-core" % "3.2.1"

  lazy val TestFrameworks = Seq( ScalaTest, ScalaCheck )

  lazy val Database = Seq (
    "com.typesafe.slick" %% "slick" % "3.0.2",
    "com.zaxxer" % "HikariCP" % "2.3.8",
    "org.mariadb.jdbc" % "mariadb-java-client" % "1.2.0"
  )

  lazy val Slick = Database ++ Seq(Flyway)

  lazy val NscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.0.0"

  lazy val ParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"

  lazy val CommonsCodec = "commons-codec" % "commons-codec" % "1.10"

  lazy val Rest = Akka ++ Slick

}
