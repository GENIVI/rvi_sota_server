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
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSResponse, WS}
import play.api.mvc.{Cookie, Cookies}
import play.api.test.Helpers._

object APITests extends Tag("APITests")

/**
 * Integration tests for the API
 */
class APIFunTests extends PlaySpec with OneServerPerSuite {

  val testVin = "TESTSTR0123456789"
  val testPackageName = "FooBarPkg"
  val testPackageVersion = "1.0.0"
  val testFilterName = "TestFilter"
  val testFilterExpression = "vin_matches '.*'"
  val testFilterAlternateExpression = "vin_matches '^VIN'"
  val testComponentName = "Radio"
  val testComponentDescription = "A radio component"
  val componentJson = Json.obj(
    "partNumber" -> testComponentName,
    "description" -> testComponentDescription
  )
  val webserverHost = Play.application.configuration.getString("test.webserver.host").get
  val webserverPort = 80 //this isn't likely to change to hardcode it instead of using an env var

  object Method extends Enumeration {
    type Method = Value
    val GET, PUT, DELETE, POST = Value
  }

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

  "test adding vins" taggedAs APITests in {
    val cookie = getLoginCookie
    val vehiclesResponse = makeRequest("vehicles/" + testVin, cookie, PUT)
    vehiclesResponse.status mustBe NO_CONTENT
  }

  "test searching vins" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("vehicles?regex=" + testVin, cookie, GET)
    searchResponse.status mustBe OK
    searchResponse.json.toString() mustEqual "[{\"vin\":\"" + testVin + "\"}]"
  }

  "test adding packages" taggedAs APITests in {
    val cookie = getLoginCookie
    val asyncHttpClient:AsyncHttpClient = WS.client.underlying
    val putBuilder = asyncHttpClient.preparePut("http://" + webserverHost + s":$webserverPort/api/v1/packages/" + testPackageName + "/" +
      testPackageVersion + "?description=test&vendor=ACME")
    val builder = putBuilder.addBodyPart(new FilePart("file", new File("../ghc-7.6.3-18.3.el7.x86_64.rpm")))
      .addHeader("Cookie", Cookies.encodeCookieHeader(cookie))
    val response = asyncHttpClient.executeRequest(builder.build()).get()
    response.getStatusCode mustBe NO_CONTENT
  }

  "test searching packages" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("packages?regex=" + testPackageName, cookie, GET)
    searchResponse.status mustBe OK
  }

  "test adding filters" taggedAs APITests in {
    val cookie = getLoginCookie
    val data = Json.obj(
      "name" -> testFilterName,
      "expression" -> testFilterExpression
    )
    val filtersResponse = makeJsonRequest("filters", cookie, POST, data)
    filtersResponse.status mustBe OK
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
    val cookie = getLoginCookie
    val data = Json.obj(
      "filterName" -> testFilterName,
      "packageName" -> testPackageName,
      "packageVersion" -> testPackageVersion
    )
    val packageFiltersResponse = makeJsonRequest("packageFilters", cookie, POST, data)
    packageFiltersResponse.status mustBe OK
    packageFiltersResponse.json.equals(data) mustBe true
  }

  "test creating components" taggedAs APITests in {
    val cookie = getLoginCookie
    val componentsResponse = makeJsonRequest("components/" + testComponentName, cookie, PUT, componentJson)
    componentsResponse.status mustBe OK
    componentsResponse.json.equals(componentJson)
  }

  "test searching components" taggedAs APITests in {
    val cookie = getLoginCookie
    val searchResponse = makeRequest("components/regex=" + testComponentName, cookie, GET)
    searchResponse.status mustBe OK
    searchResponse.json.equals(componentJson)
  }

  "test adding component to vin" taggedAs APITests in {
    val cookie = getLoginCookie
    val addComponentToVinResponse = makeRequest("vehicles/" + testVin + "/component/" + testComponentName,
      cookie, PUT)
    addComponentToVinResponse.status mustBe OK
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
}
