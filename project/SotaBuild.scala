import org.flywaydb.sbt.FlywayPlugin._
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys
import play.sbt.{PlayScala, PlaySettings}
import sbt._
import sbt.Keys._
import sbtbuildinfo._
import sbtbuildinfo.BuildInfoKeys._
import com.typesafe.sbt.packager.docker.DockerPlugin
import DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.web._


object SotaBuild extends Build {

  lazy val UnitTests = config("ut") extend Test

  lazy val IntegrationTests = config("it") extend Test

  lazy val BrowserTests = config("bt") extend Test

  lazy val RandomTests = config("rd") extend Test

  lazy val basicSettings = Seq(
    organization := "org.genivi",
    scalaVersion := "2.11.7",
    publishArtifact in Test := false,
    resolvers += Resolver.sonatypeRepo("snapshots"),

    libraryDependencies ++= Dependencies.TestFrameworks,

    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-oDS")
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
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint", "-language:higherKinds"),
    javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation")
  )

  lazy val commonSettings = basicSettings ++ compilerSettings ++ Packaging.settings ++ Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value + ".sota." + name.value.replaceAll("sota-", ""),
    buildInfoOptions ++= Seq(BuildInfoOption.ToJson, BuildInfoOption.ToMap)
  )

  // the sub-projects
  lazy val common = Project(id = "sota-common", base = file("common"))
    .settings(basicSettings ++ compilerSettings)
    .settings( libraryDependencies ++= Dependencies.Rest :+ Dependencies.AkkaHttpCirceJson :+ Dependencies.Refined :+ Dependencies.CommonsCodec)
    .dependsOn(commonData)
    .settings(Publish.settings)

  lazy val commonData = Project(id = "sota-common-data", base = file("common-data"))
    .settings(basicSettings ++ compilerSettings)
    .settings(libraryDependencies ++= Dependencies.Rest ++ Dependencies.Circe :+ Dependencies.Cats :+ Dependencies.Refined :+ Dependencies.CommonsCodec :+ Dependencies.TypesafeConfig)
    .settings(Publish.settings)

  lazy val commonTest = Project(id = "sota-common-test", base = file("common-test"))
    .settings(basicSettings ++ compilerSettings)
    .settings(libraryDependencies ++= Seq (Dependencies.Cats, Dependencies.Refined, Dependencies.Generex))
    .dependsOn(commonData)
    .settings(Publish.settings)

  lazy val commonDbTest = Project(id = "sota-common-db-test", base = file("common-db-test"))
    .settings(basicSettings ++ compilerSettings)
    .settings(libraryDependencies ++= Dependencies.Slick :+ Dependencies.Flyway :+ Dependencies.ScalaTestLib)
    .dependsOn(commonData, commonTest)
    .settings(Publish.settings)

  lazy val externalResolver = Project(id = "sota-resolver", base = file("external-resolver"))
    .settings( commonSettings ++ Migrations.settings ++ Seq(
      libraryDependencies ++= Dependencies.Rest ++ Dependencies.Circe :+ Dependencies.AkkaStream :+ Dependencies.AkkaStreamTestKit :+ Dependencies.Cats :+ Dependencies.Refined :+ Dependencies.ParserCombinators :+ Dependencies.Flyway,
      testOptions in UnitTests += Tests.Argument(TestFrameworks.ScalaTest, "-l", "RandomTest"),
      testOptions in RandomTests += Tests.Argument(TestFrameworks.ScalaTest, "-n", "RandomTest"),
      parallelExecution in Test := true,
      dockerExposedPorts := Seq(8081),
      flywayUrl := sys.env.get("RESOLVER_DB_URL").orElse( sys.props.get("resolver.db.url") ).getOrElse("jdbc:mysql://localhost:3306/sota_resolver"),
      flywayUser := sys.env.get("RESOLVER_DB_USER").orElse( sys.props.get("resolver.db.user") ).getOrElse("sota"),
      flywayPassword := sys.env.get("RESOLVER_DB_PASSWORD").orElse( sys.props.get("resolver.db.password")).getOrElse("s0ta")
    ))
    .settings(mappings in Docker += (file("deploy/wait-for-it.sh") -> "/opt/docker/wait-for-it.sh"))
    .settings(mappings in Docker += (file("deploy/entrypoint-resolver.sh") -> "/opt/docker/entrypoint.sh"))
    .settings(dockerEntrypoint := Seq("./entrypoint.sh"))
    .settings(inConfig(RandomTests)(Defaults.testTasks): _*)
    .settings(inConfig(UnitTests)(Defaults.testTasks): _*)
    .configs(RandomTests)
    .configs(UnitTests)
    .dependsOn(common, commonData, commonTest % "test", commonDbTest % "test")
    .enablePlugins(Packaging.plugins :+ BuildInfoPlugin :_*)
    .enablePlugins(BuildInfoPlugin)
    .settings(Publish.settings)

  lazy val core = Project(id = "sota-core", base = file("core"))
    .settings( commonSettings ++ Migrations.settings ++ Seq(
      libraryDependencies ++= Dependencies.Rest ++ Dependencies.Circe :+ Dependencies.Scalaz :+ Dependencies.Flyway :+ Dependencies.AmazonS3,
      testOptions in UnitTests += Tests.Argument(TestFrameworks.ScalaTest, "-l", "RequiresRvi", "-l", "IntegrationTest"),
      testOptions in IntegrationTests += Tests.Argument(TestFrameworks.ScalaTest, "-n", "RequiresRvi", "-n", "IntegrationTest"),
      parallelExecution in Test := true,
      dockerExposedPorts := Seq(8080),
      flywayUrl := sys.env.get("CORE_DB_URL").orElse( sys.props.get("core.db.url") ).getOrElse("jdbc:mysql://localhost:3306/sota_core"),
      flywayUser := sys.env.get("CORE_DB_USER").orElse( sys.props.get("core.db.user") ).getOrElse("sota"),
      flywayPassword := sys.env.get("CORE_DB_PASSWORD").orElse( sys.props.get("core.db.password")).getOrElse("s0ta")
    ))
    .settings(mappings in Docker += (file("deploy/wait-for-it.sh") -> "/opt/docker/wait-for-it.sh"))
    .settings(mappings in Docker += (file("deploy/entrypoint-core.sh") -> "/opt/docker/entrypoint.sh"))
    .settings(dockerEntrypoint := Seq("./entrypoint.sh"))
    .settings(inConfig(UnitTests)(Defaults.testTasks): _*)
    .settings(inConfig(IntegrationTests)(Defaults.testTasks): _*)
    .configs(IntegrationTests, UnitTests)
    .dependsOn(common, commonData, commonTest % "test", commonDbTest % "test", commonClient)
    .enablePlugins(Packaging.plugins: _*)
    .enablePlugins(BuildInfoPlugin)
    .settings(Publish.settings)

  import play.sbt.Play.autoImport._
  lazy val webServer = Project(id = "sota-webserver", base = file("web-server"),
    settings = commonSettings ++ PlaySettings.defaultScalaSettings ++ Seq(
      RoutesKeys.routesGenerator := InjectedRoutesGenerator,
      testOptions in UnitTests += Tests.Argument(TestFrameworks.ScalaTest, "-l", "APITests BrowserTests"),
      testOptions in IntegrationTests += Tests.Argument(TestFrameworks.ScalaTest, "-n", "APITests"),
      testOptions in BrowserTests += Tests.Argument(TestFrameworks.ScalaTest, "-n", "BrowserTests"),
      parallelExecution := false,
      parallelExecution in IntegrationTests := false,
      parallelExecution in BrowserTests := false,
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
        "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
        "org.mindrot" % "jbcrypt" % "0.3m",
        "com.unboundid" % "unboundid-ldapsdk" % "3.1.1",
        ws,
        play.sbt.Play.autoImport.cache
      ) ++ Dependencies.Database ++ Dependencies.Play2Auth
    ))
    .dependsOn(common, commonData)
    .enablePlugins(PlayScala, SbtWeb, BuildInfoPlugin)
    .settings(inConfig(UnitTests)(Defaults.testTasks): _*)
    .settings(inConfig(IntegrationTests)(Defaults.testTasks): _*)
    .settings(inConfig(BrowserTests)(Defaults.testTasks): _*)
    .configs(UnitTests, IntegrationTests, BrowserTests)
    .settings(Publish.disable)

  lazy val deviceRegistry = Project(id = "sota-device_registry", base = file("device-registry"))
    .settings(commonSettings ++ Migrations.settings ++ Seq(
      libraryDependencies ++= Dependencies.Rest ++ Dependencies.Circe :+ Dependencies.Refined :+ Dependencies.Flyway,
      parallelExecution in Test := true,
      dockerExposedPorts := Seq(8083),
      flywayUrl := sys.env.get("DEVICE_REGISTRY_DB_URL").orElse(sys.props.get("device-registry.db.url")).getOrElse("jdbc:mysql://localhost:3306/sota_device_registry"),
      flywayUser := sys.env.get("DEVICE_REGISTRY_DB_USER").orElse(sys.props.get("device-registry.db.user")).getOrElse("sota"),
      flywayPassword := sys.env.get("DEVICE_REGISTRY_DB_PASSWORD").orElse(sys.props.get("device-registry.db.password")).getOrElse("s0ta")
    ))
    .settings(inConfig(UnitTests)(Defaults.testTasks): _*)
    .configs(UnitTests)
    .dependsOn(common, commonData, commonTest % "test", commonDbTest % "test")
    .enablePlugins(Packaging.plugins :+ BuildInfoPlugin :_*)
    .settings(Publish.settings)

  lazy val commonClient = Project(id = "sota-common-client", base = file("common-client"))
    .settings(basicSettings ++ compilerSettings)
    .dependsOn(common, commonData)
    .settings(Publish.settings)

  lazy val sota = Project(id = "sota", base = file("."))
    .settings( basicSettings )
    .settings( Versioning.settings )
    .settings(Release.settings(common, commonData, commonTest, core, externalResolver, deviceRegistry, commonClient))
    .aggregate(common, commonData, commonTest, core, externalResolver, webServer, deviceRegistry, commonClient)
    .enablePlugins(Versioning.Plugin)
    .settings(Publish.disable)
}

object Dependencies {

  val AkkaVersion = "2.4.4"

  val CirceVersion = "0.4.0"

  val AkkaHttpCirceVersion = "1.6.0"

  val LogbackVersion = "1.1.3"

  val Play2AuthVersion = "0.14.2"

  val AkkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % AkkaVersion

  val AkkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion

  val AkkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % "test"

  val AkkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % AkkaVersion % "test"

  val AkkaTestKit = "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test"

  val AkkaHttpCirceJson = "de.heikoseeberger" %% "akka-http-circe" % AkkaHttpCirceVersion

  val AkkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion

  val Generex = "com.github.mifmif" % "generex" % "1.0.0"

  val Logback = "ch.qos.logback" % "logback-classic" % LogbackVersion

  lazy val Akka = Seq(
    AkkaHttp, AkkaHttpCirceJson, AkkaHttpTestKit, AkkaTestKit, AkkaSlf4j, Logback
  )

  val Circe = Seq(
    "io.circe" %% "circe-core" % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion,
    "io.circe" %% "circe-java8" % CirceVersion
  )

  lazy val Refined = "eu.timepit" %% "refined" % "0.3.1"

  lazy val Scalaz = "org.scalaz" %% "scalaz-core" % "7.1.3"
  lazy val Cats   = "org.spire-math" %% "cats" % "0.3.0"

  lazy val ScalaTestLib = "org.scalatest" %% "scalatest" % "2.2.4"

  lazy val ScalaTest = ScalaTestLib % "test"

  lazy val ScalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.4" % "test"

  lazy val Flyway = "org.flywaydb" % "flyway-core" % "3.2.1"

  lazy val TestFrameworks = Seq( ScalaTest, ScalaCheck )

  lazy val TypesafeConfig = "com.typesafe" % "config" % "1.3.0"

  lazy val Database = Seq (
    "com.typesafe.slick" %% "slick" % "3.1.1",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
    "org.mariadb.jdbc" % "mariadb-java-client" % "1.4.4"
  )

  lazy val Play2Auth = Seq(
    "jp.t2v" %% "play2-auth"        % Play2AuthVersion,
    "jp.t2v" %% "play2-auth-test"   % Play2AuthVersion % "test"
  )

  lazy val Slick = Database ++ Seq(Flyway)

  lazy val ParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"

  lazy val CommonsCodec = "commons-codec" % "commons-codec" % "1.10"

  lazy val Rest = Akka ++ Slick

  lazy val AmazonS3 =  "com.amazonaws" % "aws-java-sdk-s3" % "1.10.69"
}
