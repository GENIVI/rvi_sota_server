object Packaging {
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
  import sbt._
  import sbt.Keys._
  import com.typesafe.sbt.packager.docker.DockerPlugin
  import DockerPlugin.autoImport.Docker
  import com.typesafe.sbt.packager.Keys._
  import com.typesafe.sbt.packager.docker._

  lazy val settings = Seq(
    dockerRepository in Docker := Some("advancedtelematic"),
    packageName in Docker := packageName.value,
    dockerUpdateLatest in Docker := true,
    mappings in Docker += (file("deploy/wait-for-it.sh") -> s"/opt/${moduleName.value}/wait-for-it.sh"),
    mappings in Docker += (file(s"deploy/service_entrypoint.sh") -> s"/opt/${moduleName.value}/entrypoint.sh"),
    defaultLinuxInstallLocation in Docker := s"/opt/${moduleName.value}",
    dockerCommands := Seq(
      Cmd("FROM", "alpine:3.3"),
      Cmd("RUN", "apk upgrade --update && apk add --update openjdk8-jre bash coreutils"),
      ExecCmd("RUN", "mkdir", "-p", s"/var/log/${moduleName.value}"),
      Cmd("ADD", "opt /opt"),
      Cmd("WORKDIR", s"/opt/${moduleName.value}"),
      ExecCmd("ENTRYPOINT", s"/opt/${moduleName.value}/entrypoint.sh", moduleName.value),
      Cmd("RUN", s"chown -R daemon:daemon /opt/${moduleName.value}"),
      Cmd("RUN", s"chown -R daemon:daemon /var/log/${moduleName.value}"),
      Cmd("USER", "daemon")
    )
  )

  val plugins = Seq(DockerPlugin, JavaAppPackaging)
}
