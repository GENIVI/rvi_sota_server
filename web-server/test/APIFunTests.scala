/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

import java.io.File
import java.security.InvalidParameterException
import java.util.UUID

import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.request.body.multipart.FilePart
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalatest.Tag
import org.scalatestplus.play._
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Cookie, Cookies}
import play.api.test.Helpers._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.parser._
import org.genivi.sota.data.Device
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.data.Device._
import cats.syntax.show.toShowOps

object APITests extends Tag("APITests")

/**
 * Integration tests for the API
 *
 * These tests assume a blank, migrated database, as well as a webserver running on port 80
 */
class APIFunTests extends PlaySpec with OneServerPerSuite {

  val wsClient = app.injector.instanceOf[WSClient]
  val configuration = app.injector.instanceOf[Configuration]

  val testNamespace = "default"
  val testVin = "TESTSTR0123456789"
  val testVinAlt = "TESTALT0123456789"
  val testPackageName = "TestPkg"
  val testPackageNameAlt = "TestPkgAlt"
  val testPackageVersion = "1.0.0"
  val testFilterName = "TestFilter"
  val testFilterNameDelete = "TestDeleteFilter"
  val testFilterExpression = "vin_matches '^TEST'"
  val testFilterAlternateExpression = "vin_matches '^TESTSTR'"
  val testComponentName = "Radio"
  val testComponentNameAlt = "Satnav"
  val testComponentDescription = "A radio component"
  val testComponentDescriptionAlt = "A satellite navigation component"
  val webserverHost = configuration.getString("test.webserver.host").get
  //val webserverPort = 80 //this isn't likely to change so hardcode it instead of using an env var
  val webserverPort = port

  var testId    : Option[String] = None
  var testIdAlt : Option[String] = None

  object Method extends Enumeration {
    type Method = Value
    val GET, PUT, DELETE, POST = Value
  }

  object UpdateStatus extends Enumeration {
    type UpdateStatus = Value
    val Pending, InFlight, Canceled, Failed, Finished = Value
  }
  case class PackageId(name: String, version: String)
  case class Uri(uri: String)
  case class Package(namespace: String, id: PackageId, uri: Uri, size: Long, checkSum: String, description: String,
                     vendor: String)
  case class PackageResolver(id: PackageId, description: String, vendor: String)
  case class DeviceT(deviceName: String, deviceId: Option[String] = None, deviceType: String)
  case class FilterJson(namespace: String, name: String, expression: String)
  case class FilterPackageJson(namespace: String, filterName : String, packageName : String, packageVersion : String)
  case class ComponentJson(namespace: String, partNumber : String, description : String)
  case class UpdateRequest(namespace: String, id: String, packageId: PackageId, creationTime: String,
                           periodOfValidity: String, priority: Int, signature: String, description: String,
                           requestConfirmation: Boolean)
  import UpdateStatus._
  case class UpdateSpec(request: UpdateRequest, device: String, status: UpdateStatus, dependencies: Set[Package])
  object UpdateSpec {
    import io.circe.generic.semiauto._
    //circe fails to generate a decoder for UpdateStatus automatically, so we define one manually
    implicit val updateStatusDecoder : Decoder[UpdateStatus] = Decoder[String].map(UpdateStatus.withName)
    implicit val decoderInstace = deriveDecoder[UpdateSpec]
  }

  def getLoginCookie : Seq[Cookie] = {
    val response = await(wsClient.url("http://" + webserverHost + s":$webserverPort/authenticate")
      .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(Map("email" -> Seq("genivi"), "password" -> Seq("genivirocks!"))))
    response.status mustBe OK
    Cookies.decodeCookieHeader(response.cookies.head.toString)
  }

  import Method._
  def makeRequest(path: String, method: Method) : WSResponse = {
    val cookie = getLoginCookie
    val req = wsClient.url("http://" + webserverHost + s":$webserverPort/api/v1/" + path)
      .withHeaders("Cookie" -> Cookies.encodeCookieHeader(cookie))
    method match {
      case PUT => await(req.put(""))
      case GET => await(req.get())
      case DELETE => await(req.delete())
      case _ => throw new InvalidParameterException("POST is not supported by this method")
    }
  }

  def makeJsonRequest(path: String, method: Method, data: String) : WSResponse = {
    val cookie = getLoginCookie
    val req = wsClient.url("http://" + webserverHost + s":$webserverPort/api/v1/" + path)
      .withHeaders("Cookie" -> Cookies.encodeCookieHeader(cookie))
      .withHeaders("Content-Type" -> "application/json")
    method match {
      case PUT => await(req.put(data))
      case POST => await(req.post(data))
      case _ => throw new InvalidParameterException("POST is not supported by this method")
    }
  }

  def addDevice(deviceName: String): String = {
    val device = DeviceT(deviceName, Some(deviceName), "Vehicle")

    // create in device registry
    val response = makeJsonRequest("devices", POST, device.asJson.noSpaces)
    response.status mustBe CREATED
    val r = decode[String](response.body)
    r.toOption match {
      case Some(id: String) => id
      case None => fail("JSON parse error:" + r.toString)
    }
  }

  def addPackage(packageName: String, packageVersion: String): Unit = {
    val cookie = getLoginCookie
    val asyncHttpClient:AsyncHttpClient = wsClient.underlying
    val putBuilder = asyncHttpClient.preparePut("http://" + webserverHost + s":$webserverPort/api/v1/packages/" +
      packageName + "/" + packageVersion + "?description=test&vendor=ACME&signature=none")
    val builder = putBuilder.addBodyPart(new FilePart("file", new File("../packages/ghc-7.6.3-18.3.el7.x86_64.rpm")))
      .addHeader("Cookie", Cookies.encodeCookieHeader(cookie))
    val response = asyncHttpClient.executeRequest(builder.build()).get()
    response.getStatusCode mustBe NO_CONTENT
  }

  def addFilter(ns: String, filterName: String): Unit = {
    val data = FilterJson(ns, filterName, testFilterExpression)
    val response = makeJsonRequest("resolver/filters", POST, data.asJson.noSpaces)
    response.status mustBe OK
    val jsonResponse = decode[FilterJson](response.body)
    jsonResponse.toOption match {
      case Some(resp : FilterJson) => resp.name mustEqual filterName
                                      resp.expression mustEqual testFilterExpression
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  def addFilterToPackage(packageName : String): Unit = {
    val response =
      makeRequest("resolver/packages/" + packageName + "/" + testPackageVersion + "/filter/" + testFilterName, PUT)
    response.status mustBe OK
    val jsonResponse = decode[FilterPackageJson](response.body)
    jsonResponse.toOption match {
      case Some(resp : FilterPackageJson) => resp.filterName mustEqual testFilterName
                                             resp.packageName mustEqual packageName
                                             resp.packageVersion mustEqual testPackageVersion
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  def addComponent(ns: String, partNumber: String, description: String): Unit = {
    val data = ComponentJson(ns, partNumber, description)
    val response = makeJsonRequest("resolver/components/" + partNumber, PUT, data.asJson.noSpaces)
    response.status mustBe OK
    val jsonResponse = decode[ComponentJson](response.body)
    jsonResponse.toOption match {
      case Some(resp : ComponentJson) => resp.partNumber mustEqual partNumber
                                         resp.description mustEqual description
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test adding devices" taggedAs APITests in {
    testId = Some(addDevice(testVin))
    //add second device to aid in testing filtering later on
    testIdAlt = Some(addDevice(testVinAlt))
  }

  "test searching devices" taggedAs APITests in {
    val response = makeRequest(s"devices?namespace=$testNamespace&regex=" + testVin, GET)
    response.status mustBe OK
    val jsonResponse = decode[List[Device]](response.body)
    jsonResponse.toOption match {
      case Some(resp) => resp.length mustBe 1
        resp.headOption.map(_.id.show) mustEqual testId
      case None => fail(s"JSON parse error: $jsonResponse body: ${response.body}")
    }
  }

  "test adding packages" taggedAs APITests in {
    addPackage(testPackageName, testPackageVersion)
    //add second package to aid in testing filtering later on
    addPackage(testPackageNameAlt, testPackageVersion)
  }

  "test adding manually installed packages" taggedAs APITests in {
    val response = makeRequest("resolver/devices/" + testIdAlt.get + "/package/" + testPackageNameAlt +
      "/" + testPackageVersion, PUT)
    response.status mustBe OK
  }

  "test viewing manually installed packages" taggedAs APITests in {
    val response = makeRequest("resolver/devices/" + testIdAlt.get + "/package", GET)
    response.status mustBe OK
    val jsonResponse = decode[List[PackageId]](response.body)
    jsonResponse.toOption match {
      case Some(resp : List[PackageId]) => resp.length mustBe 1
                                           resp.head.name mustEqual testPackageNameAlt
                                           resp.head.version mustEqual testPackageVersion
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test viewing vehicles with a given package installed" taggedAs APITests in {
    val response = makeRequest("resolver/devices?packageName=" + testPackageNameAlt + "&packageVersion=" +
      testPackageVersion, GET)
    response.status mustBe OK
    val jsonResponse = decode[List[String]](response.body)
    jsonResponse.toOption match {
      case Some(resp) => resp.length mustBe 1
        resp.headOption mustEqual testIdAlt
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test searching packages" taggedAs APITests in {
    val response = makeRequest("packages?regex=^" + testPackageName + "$", GET)
    response.status mustBe OK
    val jsonResponse = decode[List[Package]](response.body)
    jsonResponse.toOption match {
      case Some(resp : List[Package]) => resp.length mustBe 1
                                         resp.head.id.name mustEqual testPackageName
                                         resp.head.id.version mustEqual testPackageVersion
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test adding filters" taggedAs APITests in {
    addFilter(testNamespace, testFilterName)
  }

  "test deleting filters" taggedAs APITests in {
    addFilter(testNamespace, testFilterNameDelete)
    val response = makeRequest("resolver/filters/" + testFilterNameDelete, DELETE)
    response.status mustBe OK
    val searchResponse = makeRequest("resolver/filters?regex=" + testFilterNameDelete, GET)
    searchResponse.status mustBe OK
    searchResponse.body.toString mustEqual "[]"
  }

  "test searching filters" taggedAs APITests in {
    val response = makeRequest("resolver/filters?regex=" + testFilterName, GET)
    response.status mustBe OK
    val jsonResponse = decode[List[FilterJson]](response.body)
    jsonResponse.toOption match {
      case Some(resp : List[FilterJson]) => resp.length mustBe 1
                                            resp.head.name mustEqual testFilterName
                                            resp.head.expression mustEqual testFilterExpression
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test changing filter expressions" taggedAs APITests in {
    val data = FilterJson(testNamespace, testFilterName, testFilterAlternateExpression)
    val response = makeJsonRequest("resolver/filters/" + testFilterName, PUT, data.asJson.noSpaces)
    response.status mustBe OK
    val jsonResponse = decode[FilterJson](response.body)
    jsonResponse.toOption match {
      case Some(resp : FilterJson) => resp.name mustEqual testFilterName
                                      resp.expression mustEqual testFilterAlternateExpression
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test adding filters to a package" taggedAs APITests in {
    addFilterToPackage(testPackageName)
  }

  "test removing filters from a package" taggedAs APITests in {
    val response = makeRequest("resolver/packages/" + testPackageName + "/" + testPackageVersion + "/filter/" +
      testFilterName, DELETE)
    response.status mustBe OK
  }

  "test re-adding filters to a package" taggedAs APITests in {
    //we also re-add the filter to test whether updates filter vins properly
    addFilterToPackage(testPackageName)
  }

  "test viewing packages with a given filter" taggedAs APITests in {
    val response = makeRequest("resolver/filters/" + testFilterName + "/package", GET)
    response.status mustBe OK
    val jsonResponse = decode[List[PackageResolver]](response.body)
    jsonResponse.toOption match {
      case Some(resp : List[PackageResolver]) =>
        resp.length mustBe 1
        resp.head.id.name mustBe testPackageName
        resp.head.id.version mustBe testPackageVersion
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test creating components" taggedAs APITests in {
    addComponent(testNamespace, testComponentName, testComponentDescription)
  }

  "test searching components" taggedAs APITests in {
    val response = makeRequest("resolver/components?regex=^" + testComponentName + "$", GET)
    response.status mustBe OK
    val jsonResponse = decode[List[ComponentJson]](response.body)
    jsonResponse.toOption match {
      case Some(resp : List[ComponentJson]) => resp.length mustBe 1
                                               resp.head.partNumber mustEqual testComponentName
                                               resp.head.description mustEqual testComponentDescription
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }

  "test deleting components" taggedAs APITests in {
    addComponent(testNamespace, testComponentNameAlt, testComponentDescriptionAlt)
    val response = makeRequest("resolver/components/" + testComponentNameAlt, DELETE)
    response.status mustBe OK
    val searchResponse = makeRequest("resolver/components?regex=" + testComponentNameAlt, GET)
    searchResponse.status mustBe OK
    searchResponse.body.toString mustEqual "[]"
  }

  "test adding component to device" taggedAs APITests in {
    addComponent(testNamespace, testComponentName, testComponentDescription)
    val response = makeRequest("resolver/devices/" + testId.get + "/component/" + testComponentName, PUT)
    response.status mustBe OK
  }

  "test viewing components installed on device" taggedAs APITests in {
    val response = makeRequest("resolver/devices/" + testId.get + "/component", GET)
    response.status mustBe OK
    val jsonResponse = decode[List[String]](response.body)
    jsonResponse.toOption match {
      case Some(resp : List[String]) => resp.length mustBe 1
                                        resp.head mustEqual testComponentName
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }


  "test listing devices with component installed" taggedAs APITests in {
    val response = makeRequest("resolver/devices?component=" + testComponentName, GET)
    response.status mustBe OK
    val jsonResponse = decode[List[String]](response.body)
    jsonResponse.toOption match {
      case Some(resp) => resp.length mustBe 1
        resp.headOption mustEqual testId
      case None =>
        fail("JSON parse error: " + jsonResponse.toString + s"body: ${response.body}")
    }
  }


  "test creating install campaigns" taggedAs APITests in {
    val cookie = getLoginCookie
    val currentTimestamp = Instant.now().toString
    val tomorrowTimestamp = Instant.now().plus(1, ChronoUnit.DAYS).toString
    val uuid = UUID.randomUUID().toString
    val updateId = UUID.randomUUID().toString
    val data = UpdateRequest(testNamespace, updateId, PackageId(testPackageName, testPackageVersion), currentTimestamp,
      currentTimestamp + "/" + tomorrowTimestamp, 1, "sig", "desc", true)
    val response = await(wsClient.url("http://" + webserverHost + s":$webserverPort/api/v1/update_requests")
      .withHeaders("Cookie" -> Cookies.encodeCookieHeader(cookie))
      .withHeaders("Content-Type" -> "application/json")
      .post(data.asJson.noSpaces))
    response.status mustBe CREATED
  }

  "test list of vins affected by update" taggedAs APITests in {
    val response = makeRequest(
      s"resolver/resolve?namespace=$testNamespace&package_name=$testPackageName&package_version=$testPackageVersion",
      GET)

    response.status mustBe OK
    import org.genivi.sota.marshalling.CirceInstances._

    val jsonResponse = decode[Map[String, Seq[PackageId]]](response.body)
    jsonResponse.toOption match {
      case Some(resp : Map[String, Seq[PackageId]]) => resp.toList.length mustBe 1
                                                    resp.headOption.map(_._1) mustBe testId
                                                    resp.head._2.length mustBe 1
                                                    resp.head._2.head.name mustBe testPackageName
                                                    resp.head._2.head.version mustBe testPackageVersion
      case None => fail("JSON parse error:" + jsonResponse.toString)
    }
  }
}
