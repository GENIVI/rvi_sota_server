/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest

import cats.implicits._

import org.genivi.sota.data.{Namespaces, PackageId, Uuid}
import org.genivi.sota.data.DeviceGenerators._
import org.genivi.sota.data.GeneratorOps._
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import org.scalatest.{FunSuite, ShouldMatchers}
import org.scalatest.concurrent.ScalaFutures

class AutoInstallResourceSpec extends FunSuite
    with ScalatestRouteTest
    with DatabaseSpec
    with ShouldMatchers
    with ScalaFutures
    with LongRequestTimeout
    with DefaultPatience
    with Generators {
  import Uri.{Path, Query}

  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)

  val service = new AutoInstallResource(db, deviceRegistry, defaultNamespaceExtractor).route
  private val autoinstallPath = "/auto_install"

  def autoinstallUrl(pathSuffixes: String*): Uri = {
    Uri.Empty.withPath(pathSuffixes.foldLeft(Path(autoinstallPath))(_/_))
  }

  def createDevice(): Uuid = {
    val device = genDevice.generate
    deviceRegistry.addDevice(device)
    device.uuid
  }

  def addDevice(pkgName: PackageId.Name, device: Uuid): HttpRequest = {
    Put(autoinstallUrl(pkgName.get, device.show))
  }

  def listDevices(pkgName: PackageId.Name): HttpRequest = {
    Get(autoinstallUrl(pkgName.get))
  }

  def listPackages(device: Uuid): HttpRequest = {
    Get(autoinstallUrl().withQuery(Query("device" -> device.show)))
  }

  def removeAll(pkgName: PackageId.Name): HttpRequest = {
    Delete(autoinstallUrl(pkgName.get))
  }

  def removeDevice(pkgName: PackageId.Name, dev: Uuid): HttpRequest = {
    Delete(autoinstallUrl(pkgName.get, dev.show))
  }


  def addDeviceOk(pkgName: PackageId.Name, device: Uuid): Unit = {
    addDevice(pkgName, device) ~> service ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  def removeAllOk(pkgName: PackageId.Name): Unit = {
    removeAll(pkgName) ~> service ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  def removeDeviceOk(pkgName: PackageId.Name, device: Uuid): Unit = {
    removeDevice(pkgName, device) ~> service ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("add device shows up in listed") {
    val device = createDevice()
    val pkgName = PackageNameGen.generate

    addDeviceOk(pkgName, device)

    listDevices(pkgName) ~> service ~> check {
      status shouldBe StatusCodes.OK
      val devs = responseAs[Seq[Uuid]]

      devs should contain(device)
    }
  }

  test("can remove device") {
    val device1 = createDevice()
    val device2 = createDevice()
    val pkgName = PackageNameGen.generate

    addDeviceOk(pkgName, device1)
    addDeviceOk(pkgName, device2)

    listDevices(pkgName) ~> service ~> check {
      status shouldBe StatusCodes.OK
      val devs = responseAs[Seq[Uuid]]

      devs should contain(device1)
    }

    removeDeviceOk(pkgName, device1)

    listDevices(pkgName) ~> service ~> check {
      status shouldBe StatusCodes.OK
      val devs = responseAs[Seq[Uuid]]

      devs should not contain(device1)
      devs should contain(device2)
    }
  }

  test("can remove all devices") {
    val device1 = createDevice()
    val device2 = createDevice()
    val pkgName = PackageNameGen.generate

    addDeviceOk(pkgName, device1)
    addDeviceOk(pkgName, device2)

    removeAllOk(pkgName)

    listDevices(pkgName) ~> service ~> check {
      status shouldBe StatusCodes.OK
      val devs = responseAs[Seq[Uuid]]

      devs shouldBe Seq()
    }
  }

  test("can't add device that doesn't exist") {
    val notDevice = Uuid.generate
    val pkgName = PackageNameGen.generate

    addDevice(pkgName, notDevice) ~> service ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  test("query packages for device") {
    val device = createDevice()
    val pkgName1 = PackageNameGen.generate
    val pkgName2 = PackageNameGen.generate

    addDeviceOk(pkgName1, device)
    addDeviceOk(pkgName2, device)

    listPackages(device) ~> service ~> check {
      val pkgs = responseAs[Seq[PackageId.Name]]
      pkgs should contain(pkgName1)
      pkgs should contain(pkgName2)
    }

  }
}
