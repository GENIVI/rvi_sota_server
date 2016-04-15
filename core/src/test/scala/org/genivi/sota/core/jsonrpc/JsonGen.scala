package org.genivi.sota.core.jsonrpc

/**
 * Trait including utilities for generating randomr JSON values
 * Used by property-based tests
 */
trait JsonGen {

  import io.circe.Json
  import org.scalacheck.Arbitrary
  import org.scalacheck.Gen

  val JBooleanGen : Gen[Json] = Gen.oneOf(true, false).map( Json.fromBoolean )
  val JStrGen : Gen[Json] = Arbitrary.arbString.arbitrary.map( Json.fromString )
  val JNumGen : Gen[Json] = Arbitrary.arbInt.arbitrary.map( Json.fromInt )
  val JNullGen: Gen[Json] = Gen.const( Json.Null )

  val JsonGen : Gen[Json] = Gen.oneOf( JBooleanGen, JStrGen, JNumGen, JNumGen)

  implicit val arbJson : Arbitrary[Json] = Arbitrary( JsonGen )

}

object JsonGen extends JsonGen
