/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import eu.timepit.refined.Refined
import org.genivi.sota.core.data.Vehicle
import org.scalacheck.{Arbitrary, Gen}

object Generators {

  val vinGen: Gen[Vehicle] = Gen.listOfN(17, Gen.alphaNumChar).map( xs => Vehicle( Refined(xs.mkString) ) )
  implicit val arbitraryVehicle : Arbitrary[Vehicle] = Arbitrary( vinGen )

}
