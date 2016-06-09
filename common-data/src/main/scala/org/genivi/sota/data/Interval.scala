package org.genivi.sota.data

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class Interval(start: DateTime, end: DateTime) {

  /**
    * See org.joda.time.base.AbstractInterval#toString()
    */
  override def toString: String = {
    val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    val s = DateTimeFormat.forPattern(pattern).print(start)
    val e = DateTimeFormat.forPattern(pattern).print(end)
    s"$s/$e"
  }

}

object Interval {

  /**
    * See org.joda.time.convert.StringConverter
    * public void setInto(ReadWritableInterval writableInterval,
    *                     Object object,
    *                     Chronology chrono)
    */
  def parse(str: String): Interval = {
    val idx = str.indexOf('/')
    val rightStr = str.substring(0, idx)
    val leftStr = str.substring(idx + 1)
    Interval(DateTime.parse(rightStr), DateTime.parse(leftStr))
  }

}
