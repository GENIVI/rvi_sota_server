package org.genivi.sota.data

import java.time.Instant

case class Interval(start: Instant, end: Instant) {

  override def toString: String = {
    s"${start.toString}/${end.toString}"
  }

}

object Interval {

  def parse(str: String): Interval = {
    val idx = str.indexOf('/')
    val rightStr = str.substring(0, idx)
    val leftStr = str.substring(idx + 1)
    Interval(Instant.parse(rightStr), Instant.parse(leftStr))
  }

}
