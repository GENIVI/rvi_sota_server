package org.genivi.sota.core

import eu.timepit.refined.Refined
import eu.timepit.refined.collection.NonEmpty
import org.genivi.sota.generic.DeepHLister
import org.genivi.sota.generic.DeepUnapply
import org.scalatest.{Matchers, WordSpec}
import shapeless._, test._

class DeepUnapplySpec extends WordSpec with Matchers {

  case class Foo(a: Refined[String, NonEmpty], b: Int)
  case class Bar(foo: Foo, c: Boolean)

  type FooRepr = String :: Int :: HNil
  type BarRepr = FooRepr :: Boolean :: HNil

  "DeepUnapply" should {
    "support nested case classes" in {
      val bar = Bar(Foo(Refined("a"), 1), false)

      val dhl = DeepHLister[Foo :: HNil]
      typed[DeepHLister.Aux[Foo :: HNil, (Refined[String, NonEmpty] :: Int :: HNil) :: HNil]]( dhl  )

      DeepUnapply.apply(bar) shouldBe Some((Refined("a"), 1, false))
    }
  }

}
