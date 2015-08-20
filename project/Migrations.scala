import org.flywaydb.sbt.FlywayPlugin

object Migrations {
  import org.flywaydb.sbt.FlywayPlugin._
  lazy val settings = flywaySettings

  val plugin = FlywayPlugin
}