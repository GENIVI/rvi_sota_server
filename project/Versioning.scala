object Versioning {
  import com.typesafe.sbt.SbtGit._
  import com.typesafe.sbt.SbtGit.GitKeys._
  import com.typesafe.sbt.GitVersioning

  lazy val settings = Seq(
    git.useGitDescribe := true,
    git.baseVersion := "0.0.1",
    git.gitDescribedVersion := gitReader.value.withGit(_.describedVersion).flatMap(v =>
      Option(v).map(_.drop(1)).orElse(formattedShaVersion.value).orElse(Some(git.baseVersion.value))
    )
  )

  val Plugin = GitVersioning

}
