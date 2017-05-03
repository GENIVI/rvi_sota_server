/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.db

import java.util.UUID
import shapeless.{::, Generic, HNil}
import slick.jdbc.MySQLProfile.api._
import scala.reflect.ClassTag

object SlickAnyVal {
  private def dbSerializableAnyValMapping[T <: AnyVal, U: BaseColumnType]
  (implicit gen: Generic.Aux[T, U :: HNil], classTag: ClassTag[T]): BaseColumnType[T] =
    MappedColumnType.base[T, U](
      (v: T) => gen.to(v).head,
      (s: U) => gen.from(s :: HNil)
    )

  // Scala implicit resolution is not smart enough to just use the method above as implicit and not use
  // the `val`s below

  implicit def stringAnyValSerializer[T <: AnyVal]
  (implicit gen: Generic.Aux[T, String :: HNil], classTag: ClassTag[T]): BaseColumnType[T] =
    dbSerializableAnyValMapping[T, String]

  implicit def uuidAnyValSerializer[T <: AnyVal]
  (implicit gen: Generic.Aux[T, UUID :: HNil], classTag: ClassTag[T]): BaseColumnType[T] =
    dbSerializableAnyValMapping[T, UUID](SlickExtensions.uuidColumnType, gen, classTag)
}
