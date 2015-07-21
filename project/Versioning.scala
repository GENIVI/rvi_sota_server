object Versioning {
  import com.typesafe.sbt.SbtGit._
  import com.typesafe.sbt.SbtGit.GitKeys._
  import com.typesafe.sbt.GitVersioning

  val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)".r

  lazy val settings = Seq(
    git.useGitDescribe := true,
    git.gitTagToVersionNumber := {
      case VersionRegex(v) => Some(v)
      case _ => None
    },
    git.baseVersion := "0.0.1",
    git.gitDescribedVersion := gitReader.value.withGit(_.describedVersion).flatMap(v =>
      Option(v).map(_.drop(1)).orElse(formattedShaVersion.value).orElse(Some(git.baseVersion.value))
    )
  )

  val Plugin = GitVersioning

}
