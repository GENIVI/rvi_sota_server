/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.generic

import eu.timepit.refined.Refined
import shapeless._

trait DeepHLister[R <: HList] extends DepFn1[R] {
  type Out <: HList
}


trait LowPriorityDeepHLister0 {
  type Aux[R <: HList, Out0 <: HList] = DeepHLister[R] {type Out = Out0} // scalastyle:ignore

  implicit def headNotCaseClassDeepHLister[H, T <: HList]
    (implicit dht: Lazy[DeepHLister[T]]): Aux[H :: T, H :: dht.value.Out] = new DeepHLister[H :: T] {
    type Out = H :: dht.value.Out

    def apply(r: H :: T): Out = r.head :: dht.value(r.tail)
  }
}

trait LowPriorityDeepHLister extends LowPriorityDeepHLister0 {

  implicit def headCaseClassDeepHLister[H, R <: HList, T <: HList]
    (implicit gen: Generic.Aux[H, R], dhh: Lazy[DeepHLister[R]], dht: Lazy[DeepHLister[T]])
      : Aux[H :: T, dhh.value.Out :: dht.value.Out] =
    new DeepHLister[H :: T] {

      type Out = dhh.value.Out :: dht.value.Out

      def apply(r: H :: T): Out = dhh.value(gen.to(r.head)) :: dht.value(r.tail)
    }

}

object DeepHLister extends LowPriorityDeepHLister {

  implicit object hnilDeepHLister extends DeepHLister[HNil] {
    type Out = HNil

    def apply(r: HNil): Out = HNil
  }

  implicit def refinedHeadDeepHLister[A, P, H, T <: HList]
    (implicit ev: H =:= Refined[A, P], dht: Lazy[DeepHLister[T]])
      : DeepHLister.Aux[Refined[A, P] :: T, Refined[A, P] :: dht.value.Out] =
    headNotCaseClassDeepHLister


  def apply[R <: HList](implicit dh: DeepHLister[R]): Aux[R, dh.Out] = dh
}
