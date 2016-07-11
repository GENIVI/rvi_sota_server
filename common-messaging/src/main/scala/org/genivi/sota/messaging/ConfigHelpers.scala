package org.genivi.sota.messaging

import cats.data.Xor
import com.typesafe.config.{Config, ConfigException}

object ConfigHelpers {

  implicit class RichConfig(config: Config) {
    def configAt(path: String): ConfigException Xor RichConfig =
      Xor.catchOnly[ConfigException](config.getConfig(path))

    def readString(path: String): ConfigException Xor String =
      Xor.catchOnly[ConfigException](config.getString(path))
  }
}
