package org.genivi.sota

/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

import java.time.Instant
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import com.advancedtelematic.jwt.{Audience, Scope, _}
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}


object Generators {

  val HmacKeySize = 16

  val SecretKeyGen: Gen[SecretKey] = Gen.containerOfN[Array, Byte](HmacKeySize, arbitrary[Byte] )
      .map( bytes => new SecretKeySpec(bytes, "HmacSHA256") )

  val TokenGen: Gen[JsonWebToken] = for {
    jti <- Gen.identifier.map(TokenId.apply)
    iss <- Gen.identifier.map(Issuer.apply)
    cid <- Gen.uuid.map(ClientId.apply)
    sub <- Gen.identifier.map(Subject.apply)
    aud <- Gen.nonEmptyContainerOf[Set, String](Gen.identifier).map(Audience.apply)
    iat <- arbitrary[Int].map( x => Instant.now().minusSeconds(x) )
    exp <- Gen.posNum[Int].map( x => Instant.now().plusSeconds(x) )
    scope <- Gen.nonEmptyContainerOf[Set, String](Gen.identifier).map(Scope.apply)
  } yield JsonWebToken(jti, iss, cid, sub, aud, iat, exp, scope)

  implicit val ArbitraryToken = Arbitrary(TokenGen)

}
