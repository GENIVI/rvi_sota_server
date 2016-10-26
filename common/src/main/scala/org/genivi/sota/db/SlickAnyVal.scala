/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.db

import shapeless.{::, Generic, HNil}
import slick.driver.MySQLDriver.api._

import scala.reflect.ClassTag

object SlickAnyVal {
  implicit def dbSerializableAnyValMapping[T <: AnyVal]
  (implicit unwrapper: Generic.Aux[T, String :: HNil], classTag: ClassTag[T]): BaseColumnType[T] =
    MappedColumnType.base[T, String](
      (v: T) => unwrapper.to(v).head,
      (s: String) => unwrapper.from(s :: HNil)
    )
}
