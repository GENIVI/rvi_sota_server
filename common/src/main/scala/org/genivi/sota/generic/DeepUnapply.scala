/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.generic

import shapeless.ops.adjoin.Adjoin
import shapeless.ops.hlist.{Tupler, IsHCons}
import shapeless.{HNil, ::, HList}

object DeepUnapply {

  def apply[T, DHL <: HList, H <: HList, HL <: HList, R](x: T)(implicit
                                                               dhl: DeepHLister.Aux[T :: HNil, DHL],
                                                               h: IsHCons.Aux[DHL, H, HNil],
                                                               adjoin: Adjoin.Aux[H, HL],
                                                               tpl: Tupler.Aux[HL, R]): Option[R] =
    Some(adjoin(dhl(x :: HNil).head).tupled)
}
