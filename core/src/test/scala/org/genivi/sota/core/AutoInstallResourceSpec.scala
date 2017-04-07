/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import org.genivi.sota.DefaultPatience
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.genivi.sota.data.{Namespace, Namespaces, PackageId, Uuid}
import org.genivi.sota.data.DeviceGenerators._
import org.genivi.sota.data.GeneratorOps._
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
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

  implicit val messageBus = MessageBusPublisher.ignore
  implicit val connectivity = DefaultConnectivity
  val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
  val service = new AutoInstallResource(db, deviceRegistry, defaultNamespaceExtractor).route
  private val autoinstallPath = "/auto_install"
  val ns = Namespace("default")

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

  def uploadPackage(pkgName: PackageId.Name): PackageId = {
    import org.genivi.sota.core.storage.StoragePipeline

    val pkgVersion = PackageVersionGen.generate
    val pkgId = PackageId(pkgName, pkgVersion)
    val pkg = Package(ns, Uuid.generate().toJava, pkgId, Uri(), 0, "", None, None, None)
    new StoragePipeline(updateService).storePackage(pkg).futureValue
    pkgId
  }

  def howManyUpdateRequestsFor(pkgId: PackageId): Int = {
    import org.genivi.sota.core.db.{Packages, UpdateRequests}
    import org.genivi.sota.refined.SlickRefined._
    import slick.driver.MySQLDriver.api._

    val pkgQ = Packages.packages
      .filter(_.name === pkgId.name)
      .filter(_.version === pkgId.version)
      .map(_.uuid)

    val q = UpdateRequests.all
      .join(pkgQ).on(_.packageUuid === _)
      .map(_._1)

    db.run(q.length.result).futureValue
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

  test("uploading package creates update request for auto_install device") {
    val device = createDevice()
    val pkgName = PackageNameGen.generate

    addDeviceOk(pkgName, device)
    val pkgId = uploadPackage(pkgName)

    howManyUpdateRequestsFor(pkgId) shouldBe 1
  }

  test("uploading package doesn't create update request for other device") {
    val device = createDevice()
    val pkgName = PackageNameGen.generate

    val pkgId = uploadPackage(pkgName)

    howManyUpdateRequestsFor(pkgId) shouldBe 0
  }
}
