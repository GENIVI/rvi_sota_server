package org.genivi.sota.resolver.test


import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.data.Device.{DeviceId, DeviceName}
import org.genivi.sota.data.{Device, DeviceT, PackageId}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import org.scalatest.concurrent.ScalaFutures
import Device._
import cats.syntax.show._

import scala.concurrent.Future


/**
 * Spec for testing Resolver REST actions
 */
class ResolveResourceSpec extends ResourceWordSpec with ScalaFutures {

  val pkgName = "resolvePkg"

  lazy val testDevices: Seq[(DeviceT, Device.Id)] = {
    Future.sequence {
      (0 to 4).map { i =>
        val d = DeviceT(DeviceName(s"Name $i"),
          Some(DeviceId(s"${i}0RES0LVEV1N12345")))

        deviceRegistry.createDevice(d).map((d, _))
      }
    }.futureValue
  }

  "Resolve resource" should {
    "return all VINs if the filter is trivially true" in {
      val deviceIds = testDevices.map(_._2)

      // Add a package.
      addPackageOK("resolvePkg", "0.0.1", None, None)

      // Add a trival filter that lets all vins through.
      addFilterOK("truefilter", "TRUE")
      addPackageFilterOK(pkgName, "0.0.1", "truefilter")

      resolveOK(pkgName, "0.0.1", deviceIds)
    }

    "support filtering by VIN" in {
      // Add another filter.
      addPackageOK("resolvePkg", "0.0.1", None, None)
      addFilterOK("0xfilter", s"""vin_matches "^00.*" OR vin_matches "^01.*"""")
      addPackageFilterOK(pkgName, "0.0.1", "0xfilter")

      val deviceIds = testDevices
        .map(e => (e._1.deviceId.get, e._2))
        .filter(e => e._1.show.startsWith("00") || e._1.show.startsWith("01"))
        .map(_._2)

      resolveOK(pkgName, "0.0.1", deviceIds)
    }

    //noinspection ZeroIndexToHead
    "support filtering by installed packages on VIN" in {
      // Delete the previous filter and add another one which uses
      // has_package instead.

      deletePackageFilterOK(pkgName, "0.0.1", "0xfilter")
      addPackageOK("apa",  "1.0.0", None, None)
      addPackageOK("bepa", "1.0.0", None, None)

      val (_, id0) = testDevices(0)
      val (_, id1) = testDevices(1)
      val (_, id2) = testDevices(2)

      installPackageOK(id0, "apa", "1.0.0")
      installPackageOK(id1, "apa", "1.0.0")
      installPackageOK(id2, "bepa", "1.0.0")

      addFilterOK("1xfilter", s"""has_package "^a.*" "1.*"""")
      addPackageFilterOK(pkgName, "0.0.1", "1xfilter")

      resolveOK(pkgName, "0.0.1", List(id0, id1))
    }

    //noinspection ZeroIndexToHead
    "support filtering by hardware components on VIN" in {

      // Delete the previous filter and add another one which uses
      // has_component instead.

      val (_, id0) = testDevices(0)
      val (_, id1) = testDevices(1)
      val (_, id2) = testDevices(2)

      deletePackageFilterOK(pkgName, "0.0.1", "1xfilter")
      addComponentOK(Refined.unsafeApply("jobby0"), "nice")
      addComponentOK(Refined.unsafeApply("jobby1"), "nice")
      installComponentOK(id0, Refined.unsafeApply("jobby0"))
      installComponentOK(id1, Refined.unsafeApply("jobby0"))
      installComponentOK(id2, Refined.unsafeApply("jobby1"))
      addFilterOK("components", s"""has_component "^.*y0"""")
      addPackageFilterOK(pkgName, "0.0.1", "components")

      resolveOK(pkgName, "0.0.1", List(id0, id1))
    }

    "return no VINs if the filter is trivially false" in {

      // Add trivially false filter.
      addFilterOK("falsefilter", "FALSE")
      addPackageFilterOK(pkgName, "0.0.1", "falsefilter")

      resolveOK(pkgName, "0.0.1", List())
    }

    "fail if a non-existing package name is given" in {

      resolve(defaultNs, "resolvePkg2", "0.0.1") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }

    "fail if a non-existing package version is given" in {

      resolve(defaultNs, pkgName, "0.0.2") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }

    //noinspection ZeroIndexToHead
    "return a string that the core server can parse" in {
      val (_, id0) = testDevices(0)
      val (_, id1) = testDevices(1)

      deletePackageFilter(pkgName, "0.0.1", "falseFilter") ~> route ~> check {
        status shouldBe StatusCodes.OK
        resolve(defaultNs, pkgName, "0.0.1") ~> route ~> check {
          status shouldBe StatusCodes.OK

          responseAs[Map[Device.Id, Set[PackageId]]] shouldBe
            Map(id0 ->
              Set(PackageId(Refined.unsafeApply(pkgName), Refined.unsafeApply("0.0.1"))),
              id1 ->
                Set(PackageId(Refined.unsafeApply(pkgName), Refined.unsafeApply("0.0.1"))))

        }
      }
    }
  }
}
