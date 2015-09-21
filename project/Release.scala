object Release {

  import sbt.Keys._
  import sbt._

  // we hide the existing definition for setReleaseVersion to replace it with our own

  import sbtrelease.ReleaseStateTransformations.{setReleaseVersion => _, _}
  import sbtrelease.ReleasePlugin.autoImport._
  import sbtrelease._

  def setVersionOnly(selectVersion: Versions => String): ReleaseStep = { st: State =>
    val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
    val selected = selectVersion(vs)

    st.log.info("Setting version to '%s'." format selected)
    val useGlobal = Project.extract(st).get(releaseUseGlobalVersion)
    val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

    reapply(Seq(
      if (useGlobal) version in ThisBuild := selected
      else version := selected
    ), st)
  }

  lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

  val showNextVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
  val showReleaseVersion = settingKey[String]("the version once releaseVersion has been applied to it")

  lazy val settings = Seq(
    showReleaseVersion <<= (version, releaseVersion)((v,f)=>f(v)),
    showNextVersion <<= (version, releaseNextVersion)((v,f)=>f(v)),

    releaseVersion <<= (releaseVersionBump)( bumper=>{
      ver => Version(ver)
          .map(_.withoutQualifier)
          .map(_.bump(bumper).string).getOrElse(versionFormatError)
    }),

    releaseProcess := Seq(
      checkSnapshotDependencies,
      releaseStepCommand(ExtraReleaseCommands.initialVcsChecksCommand),
      inquireVersions,
      setReleaseVersion,
      tagRelease,
      releaseStepCommand("core/docker:publish"),
      releaseStepCommand("resolver/docker:publish"),
      releaseStepCommand("webserver/docker:publish"),
      pushChanges
    )
  )

}