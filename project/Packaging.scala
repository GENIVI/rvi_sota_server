object Packaging {
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
  import com.typesafe.sbt.packager.docker.DockerPlugin
  import com.typesafe.sbt.packager.Keys._
  import sbt.Keys._
  import DockerPlugin.autoImport.Docker
  import com.typesafe.sbt.SbtGit.git

  lazy val settings = Seq(
    dockerRepository in Docker := Some("advancedtelematic"),
    packageName in Docker := packageName.value,
    dockerBaseImage := "advancedtelematic/java:openjdk-8-jre",
    dockerUpdateLatest in Docker := true
  )

  val plugins = Seq(DockerPlugin, JavaAppPackaging)
}
