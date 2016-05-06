/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data.client

import scala.language.implicitConversions

object ResponseEncoder {
  def apply[C, S](f: S => C): ResponseEncoder[C, S] =
    new ResponseEncoder[C, S] {
      def apply(v: S): C = f(v)
    }
}

trait ResponseEncoder[C, S] {
  def apply(v: S): C
}

object ResponseConversions {
  trait ToResponse[C] {
    def toResponse: C
  }

  trait ToResponseSeq[C] {
    def toResponse: Seq[C]
  }

  implicit def toResponseConversion[C, S](v: S)(implicit encoder: ResponseEncoder[C, S]): ToResponse[C] = {
    new ToResponse[C] {
      override def toResponse = encoder(v)
    }
  }

  implicit def toRespSeqConversion[C, S](v: Seq[S])(implicit encoder: ResponseEncoder[C, S]): ToResponseSeq[C] = {
    new ToResponseSeq[C] {
      override def toResponse = v.map(encoder(_))
    }
  }
}

