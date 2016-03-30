package org.genivi.sota.resolver.test

import eu.timepit.refined.refineV
import org.genivi.sota.data.Namespaces
import org.genivi.sota.resolver.components.Component
import org.scalacheck.{Arbitrary, Gen}

trait ComponentGenerators extends Namespaces {

  // We don't want name clashes so keep the names long.
  val genLongIdent: Gen[String] = (for {
      c <- Gen.alphaLowerChar
      n  <- Gen.choose(20, 25) // scalastyle:ignore magic.number
      cs <- Gen.listOfN(n, Gen.alphaNumChar)
    } yield (c::cs).mkString).suchThat(_.forall(c => c.isLetter || c.isDigit))

  val genPartNumber: Gen[Component.PartNumber] = {
    for (
      s0 <- genLongIdent;
      s1 = s0.substring(0, Math.min(30, s0.length)) // scalastyle:ignore magic.number
    ) yield refineV[Component.ValidPartNumber](s1).right.get
  }

  val genComponent: Gen[Component] = for {
    partNumber  <- genPartNumber
    desc        <- Gen.identifier
  } yield Component(defaultNs, partNumber, desc)

  implicit val arbComponent: Arbitrary[Component] =
    Arbitrary(genComponent)

}

object ComponentGenerators extends ComponentGenerators
