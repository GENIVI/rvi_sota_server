package org.genivi.sota.core

import cats.data.Xor
import eu.timepit.refined.Refined
import eu.timepit.refined.collection.NonEmpty
import io.circe.Decoder
import org.genivi.sota.core.Package.PackageName
import org.scalatest.{Matchers, WordSpec}

import io.circe._, io.circe.generic.auto._, io.circe.jawn._, io.circe.syntax._


case class Package(name: Package.PackageName, version: String, description: Option[String], vendor: Option[String])

object Package {

  type PackageName = String Refined NonEmpty

}

class PartialPackageSpec extends WordSpec with Matchers {

  "Partial decoder" should {
    "support refinements" in {
      val withoutName =
        """
          |{
          | "version": "1.2.3",
          | "description": "Blaa"
          |}
        """.stripMargin

      jawn.decode[PackageName => Package](withoutName).map( _(Refined[String, NonEmpty]("kernel")) ) shouldBe Xor.right( Package(Refined[String, NonEmpty]("kernel"), "1.2.3", Some("Blaa"), None) )
    }
  }

}
