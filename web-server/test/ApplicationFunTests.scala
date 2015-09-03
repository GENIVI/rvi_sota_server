/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

import java.io.File
import java.sql.SQLSyntaxErrorException

import org.openqa.selenium.By
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._
import slick.jdbc.JdbcBackend.Database

import scala.collection.JavaConversions._

class ApplicationFunTests extends PlaySpec with OneServerPerSuite with AllBrowsersPerSuite
  with BeforeAndAfterAll {

  override lazy val browsers = Vector(FirefoxInfo(firefoxProfile), ChromeInfo)
  val coreDb = Database.forConfig("core.database").createSession()
  val resolverDb = Database.forConfig("resolver.database").createSession()

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
      resolverDb.createStatement().executeQuery("delete from PackageFilters where filterName ='TestFilter'")
      resolverDb.createStatement().executeQuery("delete from Filter where name ='TestFilter'")
      resolverDb.createStatement().executeQuery("delete from Vehicle where vin = 'TESTVIN0123456789'")
      coreDb.createStatement().executeQuery("delete from Vehicle where vin = 'TESTVIN0123456789'")
      coreDb.createStatement().executeQuery("delete from Package where name = 'Testpkg'")
    } catch {
      //Teamcity handles clearing the database for us. Thus, ignoring this exception is generally
      //fine, unless you are attempting to run the integration tests locally, so we print a warning.
      case e:SQLSyntaxErrorException => println("Clearing database failed!")
    }
  }

  def sharedTests(browser: BrowserInfo) = {
    val webHost = app.configuration.getString("test.webserver.host").get
    val webPort = app.configuration.getInt("test.webserver.port").getOrElse(port)
    "All browsers" must {

      "allow users to add and search for vins " + browser.name in {
        val testVin = "TESTVIN0123456789"
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = "admin@genivi.org"
        pwdField("password").value = "genivirocks!"
        submit()
        eventually {
          click on linkText("Vehicles")
          textField("vin").value = testVin
          submit()
          eventually {
            textField("regex").value = testVin
            eventually {
              find(className("list-group-item")).value.text mustBe testVin
            }
          }
        }
      }

      "allow users to add packages " + browser.name in {
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = "admin@genivi.org"
        pwdField("password").value = "genivirocks!"
        submit()
        eventually {
          click on linkText("Packages")
          textField("name").value = "Testpkg"
          textField("version").value = "1.0.0"
          textField("description").value = "Functional test package"
          textField("vendor").value = "SOTA"
          val file = new File("../ghc-7.6.3-18.3.el7.x86_64.rpm")
          file.exists() mustBe true
          webDriver.findElement(By.name("file")).sendKeys(file.getCanonicalPath)
          submit()
          eventually {
            val elems = webDriver.findElements(By.cssSelector("span"))
            var contains = false
            for (n <- elems) if (n.getText.equals("Testpkg")) contains = true
            contains mustBe true
          }
        }
      }

      "allow users to add filters " + browser.name in {
        val filterName = "Testfilter"
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = "admin@genivi.org"
        pwdField("password").value = "genivirocks!"
        submit()
        eventually {
          click on linkText("Filters")
          textField("name").value = filterName
          textArea("expression").value = "vin_matches 'SAJNX5745SC??????'"
          submit()
          eventually {
            find(className("postStatus")).get.text mustBe "Added filter \"" + filterName +
              "\" successfully"
          }
        }
      }
    }
  }
}
