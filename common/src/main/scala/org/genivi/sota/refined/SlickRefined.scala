/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.refined

import eu.timepit.refined.internal.Wrapper
import eu.timepit.refined.{Predicate, Refined}
import shapeless.tag._

trait SlickRefined {
  import slick.driver.MySQLDriver.api._

  trait Unwrap[F[_, _]] {
    def apply[T, P](value: F[T, P]) : T
  }

  object Unwrap {

    implicit val unwrapRefined: Unwrap[Refined] = new Unwrap[Refined] {
      override def apply[T, P]( value: Refined[T, P] ): T = value.get
    }

  }

  implicit def refinedTMappedType[T, P]( implicit delegate: ColumnType[T], predicate: Predicate[P, T] ) : ColumnType[T @@ P] =
    MappedColumnType.base[T @@ P, T]( identity, x => Wrapper.tagWrapper.wrap[T, P](x) )

  implicit def refinedMappedType[T, P, F[_, _]]
      (implicit delegate: ColumnType[T],
       predicate: Predicate[P, T],
       wrapper: Wrapper[F],
       unwrapper: Unwrap[F],
       ct: scala.reflect.ClassTag[F[T, P]] ) : ColumnType[F[T, P]] =
    MappedColumnType.base[F[T, P], T]( unwrapper(_), wrapper.wrap )

}

object SlickRefined extends SlickRefined
