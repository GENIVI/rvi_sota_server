/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data.client

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.http.scaladsl.server.Directives._
import shapeless.ops.function.FnToProduct
import shapeless._
import scala.language.implicitConversions

trait RequestDecoder[C, S, A] {
  def apply(v: C, arg: A): S
}

object RequestDecoder {
  def apply[C, S, A](f: (C, A) => S): RequestDecoder[C, S, A] =
    new RequestDecoder[C, S, A] {
      def apply(v: C, args: A): S = f(v, args)
    }
}

object GenericArgsDecoder {
  def apply[C, S, L <: HList, F](f: F)
                                (implicit fp: FnToProduct.Aux[F, (C :: L) => S]): RequestDecoder[C, S, L] =
    new RequestDecoder[C, S, L] {
      def apply(v: C, args: L): S = {
        fp(f).apply(v :: args)
      }
    }
}

object RequestConversions {
  trait FromResponse[S, A] {
    def fromResponse(args: A): S
  }

  implicit def fromResponseConversion[C, S, A](v: C)(implicit decoder: RequestDecoder[C, S, A]): FromResponse[S, A] =
    new FromResponse[S, A] {
      override def fromResponse(args: A): S = decoder(v, args)
    }

  def clientEntity[C, S, A](args: A)
                           (implicit decoder: RequestDecoder[C, S, A],
                            um: FromRequestUnmarshaller[C]): Directive1[S] = {
    entity(as[C]) map { c => decoder(c, args) }
  }
}
