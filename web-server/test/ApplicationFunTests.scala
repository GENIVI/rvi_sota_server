/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

import java.io.File
import java.net.URISyntaxException

import org.openqa.selenium.By
import org.scalatestplus.play._

class ApplicationFunTests extends PlaySpec with OneServerPerSuite with AllBrowsersPerSuite {

  override lazy val browsers = Vector(FirefoxInfo(firefoxProfile), ChromeInfo)

  def sharedTests(browser: BrowserInfo) = {
    val webHost = app.configuration.getString("test.webserver.host").get
    val webPort = app.configuration.getString("test.webserver.port").getOrElse(port)
    "All browsers" must {

      "allow users to add and search for vins " + browser.name in {
        val testVin = "TESTVIN0123456789"
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = "admin@sota.com"
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

      /* Package upload is currently non-functional
      "allow users to add packages " + browser.name in {
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = "admin@sota.com"
        pwdField("password").value = "genivirocks!"
        submit()
        eventually {
          click on linkText("Packages")
          textField("name").value = "Testpkg"
          textField("version").value = "1.0"
          textField("description").value = "Functional test package"
          textField("vendor").value = "Genivi"
          val file = new File("ghc-7.6.3-18.3.el7.x86_64.rpm")
          file.exists() mustBe true

          webDriver.findElement(By.name("file")).sendKeys(file.getAbsolutePath())
          submit()
          eventually {
            cssSelector("form > span").leftSideValue mustEqual("Created package successfully")
          }
        }
      }*/

      "allow users to add filters " + browser.name in {
        val filterName = "Testfilter"
        go to (s"http://$webHost:$webPort/login")
        emailField("email").value = "admin@sota.com"
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
