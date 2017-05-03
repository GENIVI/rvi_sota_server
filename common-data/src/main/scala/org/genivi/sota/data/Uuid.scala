package org.genivi.sota.data

import cats.Show
import cats.syntax.show._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import java.util.UUID
import slick.jdbc.MySQLProfile.api._


final case class Uuid(underlying: String Refined Uuid.Valid) extends AnyVal {
  def toJava = UUID.fromString(underlying.get)
}

object Uuid {
  type Valid = eu.timepit.refined.string.Uuid

  implicit val showUuid = new Show[Uuid] {
    def show(uuid: Uuid) = uuid.underlying.get
  }

  implicit val UuidOrdering: Ordering[Uuid] = new Ordering[Uuid] {
    override def compare(uuid1: Uuid, uuid2: Uuid): Int =
      uuid1.underlying.get compare uuid2.underlying.get
  }

  def generate(): Uuid = Uuid(refineV[Valid](UUID.randomUUID.toString).right.get)

  def fromJava(uuid: UUID): Uuid = Uuid(refineV[Uuid.Valid](uuid.toString).right.get)

  // Slick mapping
  implicit val uuidColumnType =
    MappedColumnType.base[Uuid, String](_.show, (s: String) => Uuid(refineV[Uuid.Valid](s).right.get))
}
