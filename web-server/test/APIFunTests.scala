/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

import java.io.File
import java.security.InvalidParameterException
import java.util.UUID

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.multipart.FilePart
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.scalatest.Tag
import org.scalatestplus.play._
import play.api.Play
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.ws.{WSResponse, WS}
import play.api.mvc.{Cookie, Cookies}
import play.api.test.Helpers._

object APITests extends Tag("APITests")

/**
 * Integration tests for the API
 *
 * These tests assume a blank, migrated database, as well as a webserver running on port 80
 */
class APIFunTests extends PlaySpec with OneServerPerSuite {

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
  val componentJson = Json.obj(
    "partNumber" -> testComponentName,
    "description" -> testComponentDescription
  )
  val webserverHost = Play.application.configuration.getString("test.webserver.host").get
  val webserverPort = 80 //this isn't likely to change so hardcode it instead of using an env var

  object Method extends Enumeration {
    type Method = Value
    val GET, PUT, DELETE, POST = Value
  }

  case class PackageId(name: String, version: String)

  case class Vehicle(vin: String)

  implicit val packageIdReads: Reads[PackageId] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "version").read[String]
  )(PackageId.apply _)


  def getLoginCookie : Seq[Cookie] = {
    val loginResponse = await(WS.url("http://" + webserverHost + s":$webserverPort/authenticate")
      .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(Map("email" -> Seq("admin@genivi.org"), "password" -> Seq("genivirocks!"))))
    loginResponse.status mustBe OK
    Cookies.decodeCookieHeader(loginResponse.cookies.head.toString)
  }

  import Method._
  def makeRequest(path: String, cookie: Seq[Cookie], method: Method) : WSResponse = {
    val req = WS.url("http://" + webserverHost + s":$webserverPort/api/v1/" + path)
      .withHeaders("Cookie" -> Cookies.encodeCookieHeader(cookie))
    method match {
      case PUT => await(req.put(""))
      case GET => await(req.get())
      case DELETE => await(req.delete())
      case _ => throw new InvalidParameterException("POST is not supported by this method")
    }
  }

  def makeJsonRequest(path: String, cookie: Seq[Cookie], method: Method, data: JsObject) : WSResponse = {
    val req = WS.url("http://" + webserverHost + s":$webserverPort/api/v1/" + path)
      .withHeaders("Cookie" -> Cookies.encodeCookieHeader(cookie))
    method match {
      case PUT => await(req.put(data))
      case POST => await(req.post(data))
      case _ => throw new InvalidParameterException("POST is not supported by this method")
    }
  }

  def addVin(vin: String): Unit = {
    val cookie = getLoginCookie
    val vehiclesResponse = makeRequest("vehicles/" + vin, cookie, PUT)
    vehiclesResponse.status mustBe NO_CONTENT
  }

  def addPackage(packageName: String, packageVersion: String): Unit = {
    val cookie = getLoginCookie
    val asyncHttpClient:AsyncHttpClient = WS.client.underlying
    val putBuilder = asyncHttpClient.preparePut("http://" + webserverHost + s":$webserverPort/api/v1/packages/" + packageName + "/" +
      packageVersion + "?description=test&vendor=ACME")
    val builder = putBuilder.addBodyPart(new FilePart("file", new File("../packages/ghc-7.6.3-18.3.el7.x86_64.rpm")))
      .addHeader("Cookie", Cookies.encodeCookieHeader(cookie))
    val response = asyncHttpClient.executeRequest(builder.build()).get()
    response.getStatusCode mustBe NO_CONTENT
  }

  def addFilter(filterName : String): Unit = {
    val cookie = getLoginCookie
    val data = Json.obj(
      "name" -> filterName,
      "expression" -> testFilterExpression
    )
    val filtersResponse = makeJsonRequest("filters", cookie, POST, data)
    filtersResponse.status mustBe OK
    filtersResponse.json.mustEqual(data)
  }

  def addFilterToPackage(packageName : String): Unit = {
    val cookie = getLoginCookie
    val data = Json.obj(
      "filterName" -> testFilterName,
      "packageName" -> packageName,
      "packageVersion" -> testPackageVersion
    )
    val packageFiltersResponse = makeJsonRequest("packageFilters", cookie, POST, data)
    packageFiltersResponse.status mustBe OK
    packageFiltersResponse.json.equals(data) mustBe true
  }

  def addComponent(partNumber : String, description : String): Unit = {
    val cookie = getLoginCookie
    val data = Json.obj(
      "partNumber" -> partNumber,
      "description" -> description
    )
    val componentResponse = makeJsonRequest("components/" + partNumber, cookie, PUT, data)
    componentResponse.status mustBe OK
    componentResponse.json mustEqual data
  }

  "test adding vins" taggedAs APITests in {
    addVin(testVin)
    //add second vin to aid in testing filtering later on
    addVin(testVinAlt)
  }

  "test searching vins" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("vehicles?regex=" + testVin, cookie, GET)
    searchResponse.status mustBe OK
    searchResponse.json.toString() mustEqual "[{\"vin\":\"" + testVin + "\"}]"
  }

  "test adding packages" taggedAs APITests in {
    addPackage(testPackageName, testPackageVersion)
    //add second package to aid in testing filtering later on
    addPackage(testPackageNameAlt, testPackageVersion)
  }

  "test adding manually installed packages" taggedAs APITests in {
    val cookie = getLoginCookie
    val packageResponse = makeRequest("vehicles/" + testVinAlt + "/package/" + testPackageNameAlt +
      "/" + testPackageVersion, cookie, PUT)
    packageResponse.status mustBe OK
  }

  "test viewing manually installed packages" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("vehicles/" + testVinAlt + "/package", cookie, GET)
    searchResponse.status mustBe OK
    val json = Json.parse(searchResponse.body)
    json.validate[Iterable[PackageId]] match {
      case pkg: JsSuccess[Iterable[PackageId]] =>
        pkg.get.size mustBe 1
        pkg.get.head.name mustBe testPackageNameAlt
        pkg.get.head.version mustBe testPackageVersion
      case _ => fail("Invalid installed packages json received from server")
    }
  }

  "test viewing vehicles with a given package installed" taggedAs APITests in {
    val cookie = getLoginCookie
    val viewResponse = makeRequest("vehicles?packageName=" + testPackageNameAlt + "&packageVersion=" +
      testPackageVersion, cookie, GET)
    viewResponse.status mustBe OK
    //TODO: need to make sure we only get a single vin back
    (viewResponse.json \\ "vin").head.toString() mustEqual "\"" + testVinAlt + "\""
  }

  "test searching packages" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("packages?regex=^" + testPackageName + "$", cookie, GET)
    searchResponse.status mustBe OK
  }

  "test adding filters" taggedAs APITests in {
    addFilter(testFilterName)
  }

  "test deleting filters" taggedAs APITests in {
    addFilter(testFilterNameDelete)
    val cookie = getLoginCookie
    val deleteResponse = makeRequest("filters/" + testFilterNameDelete, cookie, DELETE)
    println(deleteResponse.body)
    deleteResponse.status mustBe OK
    val secondCookie = getLoginCookie
    val searchResponse = makeRequest("filters?regex=" + testFilterNameDelete, secondCookie, GET)
    searchResponse.status mustBe OK
    searchResponse.body.toString mustEqual "[]"
  }

  "test searching filters" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("filters?regex=" + testFilterName, cookie, GET)
    searchResponse.status mustBe OK
  }

  "test changing filter expressions" taggedAs APITests in {
    val cookie = getLoginCookie
    val data = Json.obj(
      "name" -> testFilterName,
      "expression" -> testFilterAlternateExpression
    )
    val filtersChangeResponse = makeJsonRequest("filters/" + testFilterName, cookie, PUT, data)
    filtersChangeResponse.status mustBe OK
  }

  "test adding filters to a package" taggedAs APITests in {
    addFilterToPackage(testPackageName)
  }

  "test removing filters from a package" taggedAs APITests in {
    val cookie = getLoginCookie
    val removeResponse = makeRequest("packageFilters/" + testPackageName + "/" + testPackageVersion + "/" +
      testFilterName, cookie, DELETE)
    removeResponse.status mustBe OK
  }

  "test re-adding filters to a package" taggedAs APITests in {
    //we also re-add the filter to test whether updates filter vins properly
    addFilterToPackage(testPackageName)
  }

  "test removing package from a filter" taggedAs APITests in {
    val cookie = getLoginCookie
    val deleteResponse = makeRequest("packageFilters/" + testPackageName + "/" + testPackageVersion + "/" +
      testFilterName, cookie, DELETE)
    deleteResponse.status mustBe OK
  }

  "test viewing packages with a given filter" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("packageFilters?filter=" + testFilterName, cookie, GET)
    searchResponse.status mustBe OK
    searchResponse.body.toString mustEqual "[]"
  }

  "test re-adding a package to a filter" taggedAs APITests in {
    addFilterToPackage(testPackageName)
  }

  "test creating components" taggedAs APITests in {
    addComponent(testComponentName, testComponentDescription)
  }

  "test searching components" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("components/regex=" + testComponentName, cookie, GET)
    searchResponse.status mustBe OK
    searchResponse.json.equals(componentJson)
  }

  "test deleting components" taggedAs APITests in {
    addComponent(testComponentNameAlt, testComponentDescriptionAlt)
    val cookie = getLoginCookie
    val deleteResponse = makeRequest("components/" + testComponentNameAlt, cookie, DELETE)
    deleteResponse.status mustBe OK
    val secondCookie = getLoginCookie
    val searchResponse = makeRequest("components?regex=" + testComponentNameAlt, secondCookie, GET)
    searchResponse.status mustBe OK
    searchResponse.body.toString mustEqual "[]"
  }

  "test adding component to vin" taggedAs APITests in {
    addComponent(testComponentName, testComponentDescription)
    val cookie = getLoginCookie
    val addResponse = makeRequest("vehicles/" + testVin + "/component/" + testComponentName, cookie, PUT)
    addResponse.status mustBe OK
  }

  "test viewing components installed on vin" taggedAs APITests in {
    val cookie = getLoginCookie
    val listResponse = makeRequest("vehicles/" + testVin + "/component", cookie, GET)
    listResponse.status mustBe OK
    //TODO: parse this body as json
    listResponse.body.toString mustEqual "[\"" + testComponentName + "\"]"
  }

  "test listing vins with component installed" taggedAs APITests in {
    val cookie = getLoginCookie
    val listResponse = makeRequest("vehicles?component=" + testComponentName, cookie, GET)
    listResponse.status mustBe OK
    //TODO: need to make sure we only get a single vin back
    (listResponse.json \\ "vin").head.toString mustEqual "\"" + testVin + "\""
  }

  "test creating install campaigns" taggedAs APITests in {
    val cookie = getLoginCookie
    val pattern = "yyyy-MM-dd'T'HH:mm:ssZZ"
    val currentTimestamp = DateTimeFormat.forPattern(pattern).print(new DateTime())
    val tomorrowTimestamp = DateTimeFormat.forPattern(pattern).print(new DateTime().plusDays(1))
    val uuid = UUID.randomUUID()
    val data = Json.obj(
      "creationTime" -> currentTimestamp,
      "id" -> uuid,
      "packageId" -> Json.obj("name" -> testPackageName, "version" -> testPackageVersion),
      "periodOfValidity" -> (currentTimestamp + "/" + tomorrowTimestamp),
      "priority" -> 1 //this could be anything from 1-10; picked at random in this case
    )
    val response = await(WS.url("http://" + webserverHost + s":$webserverPort/api/v1/updates")
      .withHeaders("Cookie" -> Cookies.encodeCookieHeader(cookie))
      .post(data))
    response.status mustBe OK
  }

  "test install queue for a vin" taggedAs APITests in {
    val cookie = getLoginCookie
    val queueResponse = makeRequest("vehicles/" + testVin + "/queued", cookie, GET)
    queueResponse.status mustBe OK
    val json = Json.parse(queueResponse.body)
    json.validate[Iterable[PackageId]] match {
      case pkg: JsSuccess[Iterable[PackageId]] =>
        pkg.get.size mustBe 1
        pkg.get.head.name mustBe testPackageName
        pkg.get.head.version mustBe testPackageVersion
      case _ => fail("Invalid installed packages json received from server")
    }
  }

  "test getting package queue for vin" taggedAs APITests in {
    val cookie = getLoginCookie
    val packageQueueResponse = makeRequest("vehicles/" + testVin + "/queued", cookie, GET)
    packageQueueResponse.status mustBe OK
    val json = Json.parse(packageQueueResponse.body)
    json.validate[Iterable[PackageId]] match {
      case pkg: JsSuccess[Iterable[PackageId]] =>
        pkg.get.size mustBe 1
        pkg.get.head.name mustBe testPackageName
        pkg.get.head.version mustBe testPackageVersion
      case _ => fail("Invalid package queue json received from server")
    }
  }

  "test list of vins affected by update" taggedAs APITests in {
    val cookie = getLoginCookie
    val listResponse = makeRequest("resolve/" + testPackageName + "/" + testPackageVersion, cookie, GET)
    listResponse.status mustBe OK
    //TODO: parse this properly. The issue is the root key for each list in the response is a vin, not a static string.
    listResponse.body.contains(testVin) && !listResponse.body.contains(testVinAlt) mustBe true
  }
}
