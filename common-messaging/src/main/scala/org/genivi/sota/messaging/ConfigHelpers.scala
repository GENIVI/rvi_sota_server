package org.genivi.sota.messaging

import cats.syntax.either._
import com.typesafe.config.{Config, ConfigException}

object ConfigHelpers {

  implicit class RichConfig(config: Config) {
    def configAt(path: String): ConfigException Either Config =
      Either.catchOnly[ConfigException](config.getConfig(path))

    def readString(path: String): ConfigException Either String =
      Either.catchOnly[ConfigException](config.getString(path))

    def readInt(path: String): ConfigException Either Int =
      Either.catchOnly[ConfigException](config.getInt(path))
  }
}
