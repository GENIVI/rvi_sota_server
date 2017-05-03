/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.refined

import eu.timepit.refined.api.{Validate, Refined}
import slick.jdbc.MySQLProfile.api._

/**
  * Map refined types to their underlaying types when interacting with
  * the database.
  */
trait SlickRefined {

  trait Wrap[F[_, _]] {
    def apply[T, P](t: T): F[T, P]
  }

  object Wrap {
    implicit val wrapRefined: Wrap[Refined] = new Wrap[Refined] {
      override def apply[T, P](t: T): Refined[T, P] = Refined.unsafeApply(t)
    }
  }

  trait Unwrap[F[_, _]] {
    def apply[T, P](value: F[T, P]) : T
  }

  object Unwrap {
    implicit val unwrapRefined: Unwrap[Refined] = new Unwrap[Refined] {
      override def apply[T, P]( value: Refined[T, P] ): T = value.get
    }
  }

  implicit def refinedMappedType[T, P, F[_, _]]
      (implicit delegate: ColumnType[T],
       p: Validate.Plain[T, P],
       wrapper: Wrap[F],
       unwrapper: Unwrap[F],
       ct: scala.reflect.ClassTag[F[T, P]] ) : ColumnType[F[T, P]] =
    MappedColumnType.base[F[T, P], T]( unwrapper(_), wrapper(_) )

}

object SlickRefined extends SlickRefined
