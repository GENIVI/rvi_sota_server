package org.genivi.sota.data

import org.scalacheck.Gen

import scala.annotation.tailrec

object GeneratorOps {
  implicit class GenSample[T](gen: Gen[T]) {
    @tailrec
    final def generate: T =
      gen.sample match {
        case Some(v) => v
        case None => generate
      }
  }
}
