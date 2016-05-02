/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data.response

import scala.language.implicitConversions

object ResponseConversions {
  trait Response

  trait ToResponse[T, R <: Response] {
    val value: T
    val conversion: T => R
    def toResponse = conversion(value)
  }

  trait ToResponseSeq[T, R <: Response] {
    val values: Seq[T]
    val conversion: T => R
    def toResponse: Seq[R] = values.map(conversion(_))
  }

  implicit def toResponseConversion[T, R <: Response](v: T)(implicit conversionFn: T => R): ToResponse[T, R] = {
    new ToResponse[T, R] {
      val value = v
      val conversion = conversionFn
    }
  }

  implicit def toRespSeqConversion[T, R <: Response](v: Seq[T])(implicit conversionFn: T => R): ToResponseSeq[T, R] = {
    new ToResponseSeq[T, R] {
      override val values = v
      override val conversion = conversionFn
    }
  }
}
