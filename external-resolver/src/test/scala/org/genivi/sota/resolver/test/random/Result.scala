package org.genivi.sota.resolver.test.random

import org.genivi.sota.data.Device._
import cats.syntax.show.toShowOps
import org.genivi.sota.data.{Device, PackageId}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.ErrorCode


sealed trait Result extends Product {
  def isEmptyResponse: Boolean = size.isEmpty
  def nonEmptyResponse: Boolean = size.nonEmpty
  def size: Option[Int] = {
    if (productArity == 0) { None }
    else {
      productElement(0) match {
        case iter: Iterable[_] => Some(iter.size)
        case _ => None
      }
    }
  }
}

final case class  Failure(c: ErrorCode)                                    extends Result
/**
  * A [[Semantics]] with [[Success]] makes [[org.genivi.sota.resolver.test.Random]]
  * check only the response's status code. To also check the response's body against expected results,
  * provide instead:
  * <ul>
  * <li>another Success case class that carries those expected results; or</li>
  * <li>a [[Failure]]</li>
  * </ul>
  */
final case object Success                                                  extends Result
final case class  AddVehicleSuccess(device: Device.Id)                     extends Result
final case class  SuccessVehicles(vehs : Set[Device.Id])                   extends Result
final case class  SuccessComponents(cs : Set[Component])                   extends Result
final case class  SuccessPartNumbers(pns: Set[Component.PartNumber])       extends Result
final case class  SuccessPackage (pkg  : Package)                          extends Result
final case class  SuccessPackages(paks : Set[Package])                     extends Result
final case class  SuccessPackageIds(pids : Set[PackageId])                 extends Result
final case class  SuccessFilters (filts: Set[Filter])                      extends Result
final case class  SuccessVehicleMap(m: Map[Device.Id, List[PackageId]])  extends Result {
  override def toString(): String = {
    val sb = new java.lang.StringBuilder("SuccessVehicleMap(")
    var isFirstElem = true
    for ( (vin, paks) <- m ) {
      if (!isFirstElem) { sb.append(", ") }
      isFirstElem = false
      sb.append(s"${vin.show} -> $paks")
    }
    sb.append(")")
    sb.toString
  }
}
