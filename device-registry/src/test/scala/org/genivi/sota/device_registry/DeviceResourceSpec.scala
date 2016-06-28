/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data.{Device, DeviceT, Namespaces, DeviceGenerators, RegexGenerators}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalacheck._


/**
 * Spec for Devices REST actions
 */
class DeviceResourceSpec extends ResourcePropSpec {

  import Arbitrary._
  import Device._
  import DeviceGenerators._
  import StatusCodes._

  def createDeviceOk(device: DeviceT): Id = {
    createDevice(device) ~> route ~> check {
      status shouldBe Created
      responseAs[Id]
    }
  }

  def deleteDeviceOk(id: Id): Unit = {
    deleteDevice(id) ~> route ~> check {
      status shouldBe OK
    }
  }

  def isRecent(time: Option[Instant]): Boolean = time match {
    case Some(time) => time.isAfter(Instant.now.minus(3, ChronoUnit.MINUTES))
    case None => false
  }

  property("GET, PUT, DELETE, and POST '/ping' request fails on non-existent device") {
    forAll { (id: Id, device: DeviceT) =>
      fetchDevice(id)          ~> route ~> check { status shouldBe NotFound }
     // updateDevice(id, device) ~> route ~> check { status shouldBe NotFound }
     // deleteDevice(id)         ~> route ~> check { status shouldBe NotFound }
     // updateLastSeen(id)       ~> route ~> check { status shouldBe NotFound }
    }
  }

  property("GET request (for Id) after POST yields same device") {
    forAll { devicePre: DeviceT =>
      val id: Id = createDeviceOk(devicePre)

      fetchDevice(id) ~> route ~> check {
        status shouldBe OK
        val devicePost: Device = responseAs[Device]
        devicePost.deviceId shouldBe devicePre.deviceId
        devicePost.deviceType shouldBe devicePre.deviceType
        devicePost.lastSeen shouldBe None
      }

      deleteDeviceOk(id)
    }
  }

  property("GET request with ?deviceId after creating yields same device.") {
    forAll { (deviceId: DeviceId, devicePre: DeviceT) =>

      val id: Id = createDeviceOk(devicePre.copy(deviceId = Some(deviceId)))
      fetchDeviceByDeviceId(defaultNs, deviceId) ~> route ~> check {
        status shouldBe OK
        val devicePost1: Device = responseAs[Device]
        fetchDevice(id) ~> route ~> check {
          status shouldBe OK
          val devicePost2: Device = responseAs[Device]

          devicePost1 shouldBe devicePost2
        }
      }
      deleteDeviceOk(id)
    }
  }

  implicit def DeviceTOrdering(implicit ord: Ordering[DeviceName]): Ordering[DeviceT] = new Ordering[DeviceT] {
    override def compare(d1: DeviceT, d2: DeviceT): Int = ord.compare(d1.deviceName, d2.deviceName)
  }

  property("GET request with ?regex yields devices which match the regex.") {

    import RegexGenerators._
    import scala.util.Random

    def injectSubstr(s: String, substr: String): String = {
      val pos = Random.nextInt(s.length)
      s.take(pos) ++ substr ++ s.drop(pos)
    }

    val numDevices = 10

    forAll(genConflictFreeDeviceTs(numDevices),
           arbitrary[String Refined Regex]) { case (devices: Seq[DeviceT],
                                                    regex: (String Refined Regex)) =>

      val n: Int = Random.nextInt(devices.length + 1)
      val regexInstances: Seq[String] = Range(0, n).map(_ => genStrFromRegex(regex))
      val preparedDevices: Seq[DeviceT] =
        Range(0, n).map { i => {
          val uniqueName = DeviceName(i.toString + injectSubstr(devices(i).deviceName.underlying, regexInstances(i)))
          devices(i).copy(deviceName = uniqueName)
        }}
      val  unpreparedDevices: Seq[DeviceT] = devices.drop(n)

      preparedDevices.length + unpreparedDevices.length shouldBe devices.length

      val created = devices.map(d => createDeviceOk(d) -> d)

      val expectedIds: Seq[Id] = created
        .filter { case (_, d) => regex.get.r.findFirstIn(d.deviceName.underlying).isDefined }
        .map(_._1)

      searchDevice(defaultNs, regex.get) ~> route ~> check {
        val matchingDevices: Seq[Device] = responseAs[Seq[Device]]
        matchingDevices.map(_.id).toSet shouldBe expectedIds.toSet
      }

      created.map(_._1).foreach(deleteDeviceOk(_))
    }
  }

  property("PUT request after POST succeeds with updated device.") {
    forAll { (devicePre1: DeviceT, devicePre2: DeviceT) =>
      val id: Id = createDeviceOk(devicePre1)

      updateDevice(id, devicePre2) ~> route ~> check {
        val updateStatus = status
        val deviceId = devicePre1.deviceId

        deviceId match {
          case Some(deviceId) =>
            fetchDeviceByDeviceId(defaultNs, deviceId) ~> route ~> check {
              status match {
                case OK => updateStatus shouldBe Conflict
                case NotFound =>
                  updateStatus shouldBe OK

                  fetchDevice(id) ~> route ~> check {
                    status shouldBe OK
                    val devicePost: Device = responseAs[Device]
                    devicePost.id shouldBe id
                    devicePost.deviceId shouldBe devicePre2.deviceId
                    devicePost.deviceType shouldBe devicePre2.deviceType
                    devicePost.lastSeen shouldBe None
                  }
                case _ => assert(false, "unexpected status code: " + status)
              }
            }
          case None => updateStatus shouldBe OK
        }
      }

      deleteDeviceOk(id)
    }
  }

  property("POST request creates a new device.") {
    forAll { devicePre: DeviceT =>
      val id: Id = createDeviceOk(devicePre)

      fetchDevice(id) ~> route ~> check {
        status shouldBe OK
        val devicePost: Device = responseAs[Device]
        devicePost.id shouldBe id
        devicePost.deviceId shouldBe devicePre.deviceId
        devicePost.deviceType shouldBe devicePre.deviceType
      }

      deleteDeviceOk(id)
    }
  }

  property("DELETE request after POST succeds and deletes device.") {
    forAll { (device: DeviceT) =>
      val id: Id = createDeviceOk(device)

      deleteDevice(id) ~> route ~> check {
        status shouldBe OK
      }

      fetchDevice(id) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  property("POST request on 'ping' should update 'lastSeen' field for device.") {
    forAll { (id: Id, devicePre: DeviceT) =>

      val id: Id = createDeviceOk(devicePre)

      updateLastSeen(id) ~> route ~> check {
        status shouldBe OK
      }

      fetchDevice(id) ~> route ~> check {
        val devicePost: Device = responseAs[Device]
        val after: Instant = Instant.now()

        devicePost.lastSeen should not be (None)
        isRecent(devicePost.lastSeen) shouldBe true
      }

      deleteDeviceOk(id)
    }
  }

  property("POST request with same deviceName fails with conflict.") {
    forAll { (device1: DeviceT, device2: DeviceT) =>

      val id: Id = createDeviceOk(device1)

      createDevice(device2.copy(deviceName = device1.deviceName)) ~> route ~> check {
        status shouldBe Conflict
      }

      deleteDeviceOk(id)
    }
  }

  property("POST request with same deviceId fails with conflict.") {
    forAll { (device1: DeviceT, device2: DeviceT) =>

      val id: Id = createDeviceOk(device1.copy(deviceName = DeviceName(device1.deviceName.underlying + "#1")))

      createDevice(device2.copy(deviceName = DeviceName(device2.deviceName.underlying + "#2"),
                                deviceId = device1.deviceId)) ~> route ~> check {
        device1.deviceId match {
          case Some(deviceId) => status shouldBe Conflict
          case None => deleteDeviceOk(responseAs[Id])
        }
      }

      deleteDeviceOk(id)
    }
  }
}
