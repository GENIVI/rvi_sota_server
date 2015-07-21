package org.genivi.sota.core

import org.joda.time.{ DateTimeZone, DateTime }
import org.joda.time.format.ISODateTimeFormat
import spray.json.deserializationError
import spray.json.{ JsString, JsValue, RootJsonFormat, DefaultJsonProtocol }

trait DateTimeJsonProtocol extends DefaultJsonProtocol {
  implicit object DateTimeJsonFormat extends RootJsonFormat[DateTime] {
    private lazy val format = ISODateTimeFormat.dateTimeNoMillis()
    def write(datetime: DateTime): JsValue = JsString(format.print(datetime.withZone(DateTimeZone.UTC)))
    def read(json: JsValue): DateTime = json match {
      case JsString(x) => try { format.parseDateTime(x) }
                          catch {
                            case _ : IllegalArgumentException => deserializationError(s"Cannot parse $x as DateTime")
                          }
      case x           => deserializationError("Expected DateTime as JsString, but got " + x)
    }
  }
}
