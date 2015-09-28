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
  val testVinName = "TESTVIN0123456789"
  val testFilterName = "TestFilter"
  val testFilterExpression = "vin_matches '.*'"
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
      resolverDb.createStatement().executeQuery("delete from PackageFilters where filterName ='" + testFilterName + "'")
      resolverDb.createStatement().executeQuery("delete from Filter where name ='" + testFilterName + "'")
      resolverDb.createStatement().executeQuery("delete from Vehicle where vin = '" + testVinName + "'")
      coreDb.createStatement().executeQuery("delete from RequiredPackages where vin = '" + testVinName + "'")
      coreDb.createStatement().executeQuery("delete from RequiredPackages where package_name = '" + testPackageName + "'")

      val rowCountResult = coreDb.createStatement().executeQuery("select count(*) as update_count from UpdateRequests where package_name = '" + testPackageName + "'")
      rowCountResult.next()
      val updateCount = rowCountResult.getInt("update_count")
      if(updateCount > 0) {
        val result = coreDb.createStatement().executeQuery("select * from UpdateRequests where package_name = '" + testPackageName + "'")
        result.next()
        val reqId = result.getString("update_request_id")
        coreDb.createStatement().executeQuery("delete from UpdateSpecs where update_request_id = '" + reqId + "'")
      }
      coreDb.createStatement().executeQuery("delete from UpdateRequests where package_name = '" + testPackageName + "'")
      coreDb.createStatement().executeQuery("delete from Vehicle where vin = '" + testVinName + "'")
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
    for (n <- elems) if (n.getText.equals(text)) contains = true
    contains
  }

  def findElementContainingText(text: String, selector: String): Boolean = {
    val elems = webDriver.findElements(By.cssSelector(selector))
    var contains = false
    for (n <- elems) if (n.getText.contains(text)) contains = true
    contains
  }

  def sharedTests(browser: BrowserInfo) = {
    val webHost = app.configuration.getString("test.webserver.host").get
    val webPort = app.configuration.getInt("test.webserver.port").getOrElse(port)
    "All browsers" must {

      "allow users to add and search for vins " + browser.name in {
        go to (s"http://$webHost:$webPort/login")
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

      "allow users to add packages " + browser.name in {
        go to (s"http://$webHost:$webPort/login")
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

      "allow users to add filters " + browser.name in {
        go to (s"http://$webHost:$webPort/login")
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

      "allow users to create install campaigns " + browser.name in {
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Packages")
          eventually {
            click on linkText(testPackageName)
            eventually {
              val filters = webDriver.findElements(By.cssSelector(".list-group-item"))
              for (n <- filters) if (n.getText.equals(testFilterName)) n.click()

              val buttons = webDriver.findElements(By.cssSelector("button"))
              for (n <- buttons) if (n.getText.equals("NEW CAMPAIGN")) n.click()
              eventually {
                numberField("priority").value = "1"
                submit()
                eventually {
                  pageSource must contain("Update ID:")
                }
              }
            }
          }
        }
      }

      "allow users to change filter expressions " + browser.name in {
        val alternateFilterExpression = "vin_matches 'TEST'"
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Filters")
          eventually {
            textField("regex").value = "^" + testFilterName + "$"
            eventually {
              val links = webDriver.findElements(By.cssSelector("a"))
              for (n <- links) if (n.getText.equals("details")) n.click()
              eventually {
                textField("expression").value = alternateFilterExpression
                submit()
                eventually {
                  findElementWithText(alternateFilterExpression, "span") mustBe true
                }
              }
            }
          }
        }
      }

      "reject invalid filter expressions " + browser.name in {
        val alternateFilterExpression = "invalid"
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = userName
        pwdField("password").value = password
        submit()
        eventually {
          click on linkText("Filters")
          eventually {
            textField("regex").value = "^" + testFilterName + "$"
            eventually {
              val links = webDriver.findElements(By.cssSelector("a"))
              for (n <- links) if (n.getText.equals("details")) n.click()
              eventually {
                textField("expression").value = alternateFilterExpression
                submit()
                eventually {
                  findElementContainingText("Predicate failed:", "div") mustBe true
                }
              }
            }
          }
        }
      }

      "allow users to delete filters " + browser.name in {
        val testDeleteFilterName = "TestDeleteFilter"
        go to (s"http://$webHost:$webPort/login")
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
            eventually {
              val links = webDriver.findElements(By.cssSelector("a"))
              for (n <- links) if (n.getText.equals("details")) n.click()

              val buttons = webDriver.findElements(By.cssSelector("button"))
              for (n <- buttons) if (n.getText.equals("Delete Filter")) n.click()
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
}
