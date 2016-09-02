/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data.client

import shapeless.HList
import shapeless.ops.function.FnToProduct
import shapeless._

import scala.language.implicitConversions

object ResponseEncoder {
  def apply[S, C](f: S => C): ResponseEncoder[S, C, HNil] =
    new ResponseEncoder[S, C, HNil] {
      def apply(v: S, args: HNil): C = f(v)
    }
}

object GenericResponseEncoder {
  def apply[C, S, L <: HList, F](f: F)
                                (implicit fp: FnToProduct.Aux[F, (S :: L) => C]): ResponseEncoder[S, C, L] =
    new ResponseEncoder[S, C, L] {
      def apply(v: S, args: L): C = {
        fp(f).apply(v :: args)
      }
    }
}

trait ResponseEncoder[S, C, A] {
  def apply(v: S, args: A): C
}

object ResponseConversions {
  trait ToResponse[C] {
    def toResponse: C
  }

  trait ToResponseSeq[C] {
    def toResponse: Seq[C]
  }

  trait ToResponseGenericSeq[C] {
    def toResponse: Seq[C]
  }

  trait ToResponseGenericUnary[C, A] {
    def toResponse(args: A): C
  }

  trait ToResponseGeneric[C, T, L] {
    def toResponse(args: T)(implicit gen: Generic.Aux[T, L]): C
  }

  implicit def toRespGenericSeqNoArgsConversion[C, S](v: Seq[S])
                                               (implicit encoder: ResponseEncoder[S, C, HNil]): ToResponseSeq[C] = {
    new ToResponseSeq[C] {
      override def toResponse = v.map(encoder(_, HNil))
    }
  }

  implicit def toRespGenericSeqConversion[S, C, A](v: Seq[(S, A)])
                                                  (implicit encoder: ResponseEncoder[S, C, A])
  : ToResponseGenericSeq[C] = {
    new ToResponseGenericSeq[C] {
      override def toResponse = v.map { case (elem, args) => encoder(elem, args) }
    }
  }

  implicit def toRespGenericSeqUnaryConversion[S, C, A, L](v: Seq[(S, A)])
                                                  (implicit encoder: ResponseEncoder[S, C, L],
                                                   evidence: (A :: HNil) =:= L)
  : ToResponseGenericSeq[C] =
    new ToResponseGenericSeq[C] {
      override def toResponse = v.map { case (elem, arg) => encoder(elem, evidence(arg :: HNil))
      }
    }

  implicit def toResponseGenericUnaryConversion[S, C, A, L](v: S)
                                                           (implicit encoder: ResponseEncoder[S, C, L],
                                                            evidence: (A :: HNil) =:= L)
  : ToResponseGenericUnary[C, A] = {
    new ToResponseGenericUnary[C, A] {
      override def toResponse(arg: A) = encoder(v, evidence(arg :: HNil))
    }
  }

  implicit def toResponseGenericConversionTuple[S, C, T <: Product, L](v: S)(implicit encoder: ResponseEncoder[S, C, L])

  : ToResponseGeneric[C, T, L] = {
    new ToResponseGeneric[C, T, L] {
      override def toResponse(args: T)(implicit gen: Generic.Aux[T, L]): C = encoder(v, gen.to(args))
    }
  }
}
