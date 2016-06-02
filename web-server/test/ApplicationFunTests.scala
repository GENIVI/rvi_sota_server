/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

import java.io.File
import java.sql.SQLSyntaxErrorException

import org.openqa.selenium.By
import org.scalatest.{Tag, BeforeAndAfterAll}
import org.scalatestplus.play._
import slick.jdbc.JdbcBackend.Database

import scala.collection.JavaConversions._

object BrowserTests extends Tag("BrowserTests")

class ApplicationFunTests extends PlaySpec with OneServerPerSuite with AllBrowsersPerSuite
  with BeforeAndAfterAll {

  override lazy val browsers = Vector(FirefoxInfo(firefoxProfile), ChromeInfo)
  val coreDb = Database.forConfig("core.database").createSession()
  val resolverDb = Database.forConfig("resolver.database").createSession()
  val testVinName = "TESTVIN0123456789"
  val testFilterName = "TestFilter"
  val testFilterExpression = "vin_matches '.*'"
  val testDeleteFilterName = "TestDeleteFilter"
  val testPackageName = "Testpkg"
  val userName = "admin@genivi.org"
  val password = "genivirocks!"

  override def beforeAll() {
    clearTables()
  }

  override def afterAll() {
    clearTables()
    coreDb.close()
    resolverDb.close()
  }

  def clearTables() {
    try {
      resolverDb.createStatement().executeQuery("delete from PackageFilter where filterName ='" + testFilterName + "'")
      resolverDb.createStatement().executeQuery("delete from Filter where name ='" + testFilterName + "'")
      resolverDb.createStatement().executeQuery("delete from Filter where name ='" + testDeleteFilterName + "'")
      resolverDb.createStatement().executeQuery("delete from Vehicle where vin = '" + testVinName + "'")
      coreDb.createStatement().executeQuery("delete from RequiredPackage where vin = '" + testVinName + "'")
      coreDb.createStatement().executeQuery("delete from RequiredPackage where package_name = '" + testPackageName
        + "'")

      val rowCountResult = coreDb.createStatement().executeQuery(
        "select count(*) as update_count from UpdateRequest where package_name = '" + testPackageName + "'")
      rowCountResult.next()
      val updateCount = rowCountResult.getInt("update_count")
      if(updateCount > 0) {
        val result = coreDb.createStatement().executeQuery(
          "select * from UpdateRequest where package_name = '" + testPackageName + "'")
        while(result.next()) {
          val reqId = result.getString("update_request_id")
          coreDb.createStatement().executeQuery("delete from UpdateSpec where update_request_id = '" + reqId + "'")
        }
      }
      coreDb.createStatement().executeQuery("delete from UpdateRequest where package_name = '" + testPackageName + "'")
      coreDb.createStatement().executeQuery("delete from Package where name = '" + testPackageName + "'")
    } catch {
      //Teamcity handles clearing the database for us. Thus, ignoring this exception is generally
      //fine, unless you are attempting to run the integration tests locally.
      case e:SQLSyntaxErrorException => println("Clearing database failed!\nException msg:" + e.getMessage)
    }
  }

  def findElementWithText(text: String, selector: String): Boolean = {
    val elems = webDriver.findElements(By.cssSelector(selector))
    var contains = false
    for (n <- elems) if (n.getText.equalsIgnoreCase(text)) contains = true
    contains
  }

  def findElementContainingText(text: String, selector: String): Boolean = {
    val elems = webDriver.findElements(By.cssSelector(selector))
    var contains = false
    for (n <- elems) if (n.getText.contains(text)) contains = true
    contains
  }

  def sharedTests(browser: BrowserInfo) {
    val webHost = app.configuration.getString("test.webserver.host").get
    val webPort = app.configuration.getInt("test.webserver.port").getOrElse(port)
    "All browsers" must {

      "allow users to add and search for vins " + browser.name taggedAs BrowserTests in {
        go to s"http://$webHost:$webPort/login"
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Vehicles")
          click on cssSelector("button")
          textField("vin").value = testVinName
          submit()
          eventually {
            textField("regex").value = testVinName
            findElementWithText(testVinName, "td") mustBe true
          }
        }
      }

      "allow users to add packages " + browser.name taggedAs BrowserTests in {
        go to s"http://$webHost:$webPort/login"
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Packages")
          click on cssSelector("button")
          textField("name").value = testPackageName
          textField("version").value = "1.0.0"
          textField("description").value = "Functional test package"
          textField("vendor").value = "SOTA"
          val file = new File("../ghc-7.6.3-18.3.el7.x86_64.rpm")
          file.exists() mustBe true
          webDriver.findElement(By.name("file")).sendKeys(file.getCanonicalPath)
          submit()
          eventually {
            textField("regex").value = testPackageName
            findElementWithText(testPackageName, "a") mustBe true
          }
        }
      }

      "allow users to add filters " + browser.name taggedAs BrowserTests in {
        go to s"http://$webHost:$webPort/login"
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Filters")
          click on cssSelector("button")
          textField("name").value = testFilterName
          textArea("expression").value = testFilterExpression
          submit()
          eventually {
            textField("regex").value = testFilterName
            findElementWithText(testFilterName, "td") mustBe true
          }
        }
      }

      "allow users to create install campaigns " + browser.name taggedAs BrowserTests in {
        go to s"http://$webHost:$webPort/login"
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Packages")
          click on linkText(testPackageName)
          click on testFilterName
          click on "new-campaign"
          numberField("priority").value = "1"
          submit()
          eventually {
            findElementContainingText("Update ID:", "span") mustBe true
          }
        }
      }

      "allow users to change filter expressions " + browser.name taggedAs BrowserTests in {
        val alternateFilterExpression = "vin_matches 'TEST'"
        go to s"http://$webHost:$webPort/login"
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Filters")
          textField("regex").value = "^" + testFilterName + "$"
          click on linkText("Details")
          textField("expression").value = alternateFilterExpression
          submit()
          eventually {
            findElementWithText(alternateFilterExpression, "span") mustBe true
          }
        }
      }

      "reject invalid filter expressions " + browser.name taggedAs BrowserTests in {
        val alternateFilterExpression = "invalid"
        go to s"http://$webHost:$webPort/login"
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Filters")
          textField("regex").value = "^" + testFilterName + "$"
          click on linkText("Details")
          textField("expression").value = alternateFilterExpression
          submit()
          eventually {
            findElementContainingText("Predicate failed:", "div") mustBe true
          }
        }
      }

      "allow users to delete filters " + browser.name taggedAs BrowserTests in {
        go to s"http://$webHost:$webPort/login"
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Filters")
          click on cssSelector("button")
          textField("name").value = testDeleteFilterName
          textArea("expression").value = testFilterExpression
          submit()
          eventually {
            textField("regex").value = "^" + testDeleteFilterName + "$"
            click on linkText("Details")
            click on "delete-filter"
            eventually {
              textField("regex").value = "^" + testDeleteFilterName + "$"
              eventually {
                findElementWithText(testDeleteFilterName, "td") mustBe false
              }
            }
          }
        }
      }
    }
  }
}
