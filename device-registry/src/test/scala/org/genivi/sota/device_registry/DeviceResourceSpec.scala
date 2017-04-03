/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import java.time.OffsetDateTime
import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.StatusCodes._
import eu.timepit.refined.api.Refined
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data._
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.device_registry.common.PackageStat
import org.genivi.sota.device_registry.daemon.DeviceSeenListener
import org.genivi.sota.device_registry.db.InstalledPackages
import org.genivi.sota.device_registry.db.InstalledPackages.{DevicesCount, InstalledPackage}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.DeviceSeen
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures


/**
 * Spec for DeviceRepository REST actions
 */
class DeviceResourceSpec extends ResourcePropSpec with ScalaFutures with SlickExtensions {

  import Device._
  import GeneratorOps._

  private val deviceNumber = defaultLimit + 10
  private implicit val exec = system.dispatcher
  private val publisher = DeviceSeenListener.action(MessageBusPublisher.ignore)(_)

  def isRecent(time: Option[Instant]): Boolean = time match {
    case Some(t) => t.isAfter(Instant.now.minus(3, ChronoUnit.MINUTES))
    case None => false
  }

  private def sendDeviceSeen(uuid: Uuid, lastSeen: Instant = Instant.now()): Unit =
      publisher(DeviceSeen(defaultNs, uuid, Instant.now())).futureValue

  property("GET, PUT, DELETE, and POST '/ping' request fails on non-existent device") {
    forAll { (uuid: Uuid, device: DeviceT, json: Json) =>
      fetchDevice(uuid)          ~> route ~> check { status shouldBe NotFound }
      updateDevice(uuid, device) ~> route ~> check { status shouldBe NotFound }
    }
  }

  property("GET request (for Id) after POST yields same device") {
    forAll { devicePre: DeviceT =>
      val uuid: Uuid = createDeviceOk(devicePre)

      fetchDevice(uuid) ~> route ~> check {
        status shouldBe OK
        val devicePost: Device = responseAs[Device]
        devicePost.deviceId shouldBe devicePre.deviceId
        devicePost.deviceType shouldBe devicePre.deviceType
        devicePost.lastSeen shouldBe None
      }
    }
  }

  property("GET request with ?deviceId after creating yields same device.") {
    forAll { (deviceId: DeviceId, devicePre: DeviceT) =>

      val uuid: Uuid = createDeviceOk(devicePre.copy(deviceId = Some(deviceId)))
      fetchByDeviceId(defaultNs, deviceId) ~> route ~> check {
        status shouldBe OK
        val devicePost1: Device = responseAs[Seq[Device]].head
        fetchDevice(uuid) ~> route ~> check {
          status shouldBe OK
          val devicePost2: Device = responseAs[Device]

          devicePost1 shouldBe devicePost2
        }
      }
    }
  }

  property("PUT request after POST succeeds with updated device.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val uuid: Uuid = createDeviceOk(d1)

      updateDevice(uuid, d2) ~> route ~> check {
        val updateStatus = status

        d1.deviceId match {
          case Some(deviceId) =>
            fetchByDeviceId(defaultNs, deviceId) ~> route ~> check {
              status match {
                case OK => responseAs[Seq[Device]].headOption match {
                  case Some(_) => updateStatus shouldBe Conflict
                  case None =>
                    updateStatus shouldBe OK

                    fetchDevice(uuid) ~> route ~> check {
                      status shouldBe OK
                      val devicePost: Device = responseAs[Device]
                      devicePost.uuid shouldBe uuid
                      devicePost.deviceId shouldBe d2.deviceId
                      devicePost.deviceType shouldBe d2.deviceType
                      devicePost.lastSeen shouldBe None
                    }
                }
                case _ => assert(false, "unexpected status code: " + status)
              }
            }
          case None => updateStatus shouldBe OK
        }
      }
    }
  }

  property("POST request creates a new device.") {
    forAll { devicePre: DeviceT =>

      val uuid: Uuid = createDeviceOk(devicePre)

      fetchDevice(uuid) ~> route ~> check {
        status shouldBe OK
        val devicePost: Device = responseAs[Device]
        devicePost.uuid shouldBe uuid
        devicePost.deviceId shouldBe devicePre.deviceId
        devicePost.deviceType shouldBe devicePre.deviceType
      }
    }
  }

  property("POST request on 'ping' should update 'lastSeen' field for device.") {
    forAll { (uuid: Uuid, devicePre: DeviceT) =>

      val uuid: Uuid = createDeviceOk(devicePre)

      sendDeviceSeen(uuid)

      fetchDevice(uuid) ~> route ~> check {
        val devicePost: Device = responseAs[Device]

        devicePost.lastSeen should not be None
        isRecent(devicePost.lastSeen) shouldBe true
      }
    }
  }

  property("POST request with same deviceName fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val name = arbitrary[DeviceName].sample.get
      val uuid: Uuid = createDeviceOk(d1.copy(deviceName = name))

      createDevice(d2.copy(deviceName = name)) ~> route ~> check {
        status shouldBe Conflict
      }
    }
  }

  property("POST request with same deviceId fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val uuid: Uuid = createDeviceOk(d1)

      createDevice(d2.copy(deviceId = d1.deviceId)) ~> route ~> check {
        d1.deviceId match {
          case Some(deviceId) => status shouldBe Conflict
          case None => status shouldBe Created
        }
      }
    }
  }

  property("First POST request on 'ping' should update 'activatedAt' field for device.") {
    forAll { (uuid: Uuid, devicePre: DeviceT) =>

      val uuid: Uuid = createDeviceOk(devicePre)

      sendDeviceSeen(uuid)

      fetchDevice(uuid) ~> route ~> check {
        val firstDevice = responseAs[Device]

        val firstActivation = firstDevice.activatedAt
        firstActivation should not be None
        isRecent(firstActivation) shouldBe true

        fetchDevice(uuid) ~> route ~> check {
          val secondDevice = responseAs[Device]

          secondDevice.activatedAt shouldBe firstActivation
        }
      }
    }
  }

  property("POST request on ping gets counted") {
    forAll { (uuid: Uuid, devicePre: DeviceT) =>

      val start = OffsetDateTime.now()
      val uuid: Uuid = createDeviceOk(devicePre)
      val end = start.plusHours(1)

      sendDeviceSeen(uuid)

      getActiveDeviceCount(start, end) ~> route ~> check {
        responseAs[ActiveDeviceCount].deviceCount shouldBe 1
      }
    }
  }

  property("PUT request updates device.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1: DeviceT, d2: DeviceT) =>

      val uuid: Uuid = createDeviceOk(d1)

      updateDevice(uuid, d2) ~> route ~> check {
        status shouldBe OK
        fetchDevice(uuid) ~> route ~> check {
          status shouldBe OK
          val updatedDevice: Device = responseAs[Device]
          updatedDevice.deviceId shouldBe d2.deviceId
          updatedDevice.deviceType shouldBe d2.deviceType
          updatedDevice.lastSeen shouldBe None
        }
      }
    }
  }

  property("PUT request does not update last seen") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1: DeviceT, d2: DeviceT) =>

      val uuid: Uuid = createDeviceOk(d1)

      sendDeviceSeen(uuid)

      updateDevice(uuid, d2) ~> route ~> check {
        status shouldBe OK
        fetchDevice(uuid) ~> route ~> check {
          status shouldBe OK
          val updatedDevice: Device = responseAs[Device]
          updatedDevice.lastSeen shouldBe defined
        }
      }
    }
  }

  property("PUT request with same deviceName fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val uuid1: Uuid = createDeviceOk(d1)
      val uuid2: Uuid = createDeviceOk(d2)

      updateDevice(uuid1, d1.copy(deviceName = d2.deviceName)) ~> route ~> check {
        status shouldBe Conflict
      }
    }
  }

  property("PUT request with same deviceId fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val deviceId = arbitrary[DeviceId].suchThat(_ != d1.deviceId).sample.get
      val uuid1: Uuid = createDeviceOk(d1)
      val uuid2: Uuid = createDeviceOk(d2.copy(deviceId = Some(deviceId)))

      updateDevice(uuid1, d1.copy(deviceId = Some(deviceId))) ~> route ~> check {
        d2.deviceId match {
          case Some(_) => status shouldBe Conflict
          case None => ()
        }
      }
    }
  }

  property("Can install packages on a device") {
    forAll { (device: DeviceT, pkg: PackageId) =>
      val uuid = createDeviceOk(device)

      installSoftware(uuid, Set(pkg)) ~> route ~> check {
        status shouldBe NoContent
      }

      listPackages(uuid) ~> route ~> check {
        status shouldBe OK
        val response = responseAs[Seq[InstalledPackage]]
        response.length shouldBe 1
        response.head.packageId shouldEqual pkg
        response.head.device shouldBe uuid
      }
    }
  }

  property("Can filter list of installed packages on a device") {
    val uuid = createDeviceOk(genDeviceT.generate)
    val pkgs = List(PackageId(Refined.unsafeApply("foo"), Refined.unsafeApply("1.0.0")),
      PackageId(Refined.unsafeApply("bar"), Refined.unsafeApply("1.0.0")))

    installSoftware(uuid, pkgs.toSet) ~> route ~> check {
      status shouldBe NoContent
    }

    listPackages(uuid, Some("foo")) ~> route ~> check {
      status shouldBe OK
      val response = responseAs[Seq[InstalledPackage]]
      response.length shouldBe 1
      response.head.packageId shouldEqual pkgs.head
      response.head.device shouldBe uuid
    }
  }

  property("Can get stats for a package") {
    val deviceNumber = 20
    val groupNumber = 5
    val deviceTs = genConflictFreeDeviceTs(deviceNumber).sample.get
    val groups = Gen.listOfN(groupNumber, genGroupName).sample.get
    val pkg = genPackageId.sample.get

    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))
    val groupIds: Seq[Uuid] = groups.map(createGroupOk(_))

    (0 until deviceNumber).foreach { i =>
      addDeviceToGroupOk(groupIds(i % groupNumber), deviceIds(i))
    }
    deviceIds.foreach(device => installSoftwareOk(device, Set(pkg)))

    getStatsForPackage(pkg) ~> route ~> check {
      status shouldBe OK
      val resp = responseAs[DevicesCount]
      resp.deviceCount shouldBe deviceNumber
      //convert to sets as order isn't important
      resp.groupIds shouldBe groupIds.toSet
    }
  }

  property("can list devices with custom pagination limit") {
    val limit = 30
    val deviceTs = genConflictFreeDeviceTs(deviceNumber).sample.get
    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))

    searchDevice(defaultNs, "", limit = limit) ~> route ~> check {
      status shouldBe OK
      val result = responseAs[Seq[Device]]
      result.length shouldBe limit
    }
  }

  property("can list devices with custom pagination limit and offset") {
    val limit = 30
    val offset = 10
    val deviceTs = genConflictFreeDeviceTs(deviceNumber).sample.get
    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))

    searchDevice(defaultNs, "", offset = offset, limit = limit) ~> route ~> check {
      status shouldBe OK
      val devices = responseAs[Seq[Device]]
      devices.length shouldBe limit
      devices.zip(devices.tail).foreach { case (device1, device2) =>
        device1.deviceName.underlying.compareTo(device2.deviceName.underlying) should be <= 0
      }
    }
  }

  property("can list devices by group ID") {
    val limit = 30
    val offset = 10
    val deviceNumber = 50
    val deviceTs = genConflictFreeDeviceTs(deviceNumber).sample.get
    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))
    val group = genGroupName.sample.get
    val groupId = createGroupOk(group)

    deviceIds.foreach { id => addDeviceToGroupOk(groupId, id) }

    // test that we get back all the devices
    fetchByGroupId(defaultNs, groupId, offset = 0, limit = deviceNumber) ~> route ~> check {
      status shouldBe OK
      val devices = responseAs[Seq[Device]]
      devices.length shouldBe deviceNumber
      devices.map(_.uuid).toSet shouldBe deviceIds.toSet
    }

    // test that the limit works
    fetchByGroupId(defaultNs, groupId, offset = offset, limit = limit) ~> route ~> check {
      status shouldBe OK
      val devices = responseAs[Seq[Device]]
      devices.length shouldBe limit
    }
  }

  property("can list installed packages for all devices with custom pagination limit and offset") {

    val limit = 30
    val offset = 10

    val deviceTs = genConflictFreeDeviceTs(deviceNumber).generate
    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))

    // the database is case-insensitve so when we need to take that in to account when sorting in scala
    // furthermore PackageId is not lexicographically ordered so we just use pairs
    def canonPkg(pkg: PackageId) =
      (pkg.name.get.toLowerCase, pkg.version.get)

    val commonPkg = genPackageId.generate

    // get packages directly through the DB without pagination
    val beforePkgsAction = InstalledPackages.getInstalledForAllDevices(defaultNs)
    val beforePkgs = db.run(beforePkgsAction).futureValue.map(canonPkg)

    val allDevicesPackages = deviceIds.map {device =>
      val pkgs = Gen.listOf(genPackageId).generate.toSet + commonPkg
      installSoftwareOk(device, pkgs)
      pkgs
    }

    val allPackages =
      (allDevicesPackages.map(_.map(canonPkg)).toSet.flatten ++ beforePkgs.toSet)
        .toSeq.sorted

    getInstalledForAllDevices(offset=offset, limit=limit) ~> route ~> check {
      status shouldBe OK
      val paginationResult = responseAs[PaginatedResult[PackageId]]
      paginationResult.total shouldBe allPackages.length
      paginationResult.limit shouldBe limit
      paginationResult.offset shouldBe offset
      val packages = paginationResult.values.map(canonPkg)
      packages.length shouldBe scala.math.min(limit, allPackages.length)
      packages shouldBe sorted
      packages shouldBe allPackages.drop(offset).take(limit)
    }
  }

  property("Posting to affected packages returns affected devices") {
    forAll { (device: DeviceT, p: PackageId) =>
      val uuid = createDeviceOk(device)

      installSoftwareOk(uuid, Set(p))

      getAffected(Set(p)) ~> route ~> check {
        status shouldBe OK
        responseAs[Map[Uuid, Seq[PackageId]]] should contain(uuid -> Seq(p))
      }
    }
  }

  property("Package stats correct reports number of installed instances") {
    val devices = genConflictFreeDeviceTs(10).sample.get
    val pkgName = genPackageIdName.sample.get
    val pkgVersion = genConflictFreePackageIdVersion(2)

    val uuids = devices.map(createDeviceOk(_))
    uuids.zipWithIndex.foreach { case (uuid, i) =>
      if(i % 2 == 0) {
        installSoftwareOk(uuid, Set(PackageId(pkgName, pkgVersion.head)))
      } else {
        installSoftwareOk(uuid, Set(PackageId(pkgName, pkgVersion(1))))
      }
    }
    getPackageStats(pkgName) ~> route ~> check {
      status shouldBe OK
      val r = responseAs[PaginatedResult[PackageStat]]
      r.total shouldBe 2
      r.values.contains(PackageStat(pkgVersion.head, 5)) shouldBe true
      r.values.contains(PackageStat(pkgVersion(1), 5)) shouldBe true
    }
  }
}
