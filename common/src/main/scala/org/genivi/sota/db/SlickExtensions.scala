/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.db

import java.sql.Timestamp
import java.util.UUID

import akka.http.scaladsl.model.Uri
import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid

import slick.ast.{Node, TypedType}
import slick.driver.MySQLDriver.api._
import slick.lifted.Rep

/**
  * Explain to the database layer, Slick, how to map Uri and UUIDs into
  * types that the database knows about (strings).
  *
  * @see {@link http://slick.typesafe.com/docs/}
  */

object SlickExtensions {
  implicit val UriColumnType = MappedColumnType.base[Uri, String](_.toString(), Uri.apply)

  implicit val uuidColumnType = MappedColumnType.base[UUID, String]( _.toString(), UUID.fromString )

  /**
    * Define how to store a [[java.time.Instant]] in the SQL database.
    */
  implicit val javaInstantMapping = {
    MappedColumnType.base[Instant, Timestamp](
      dt => new Timestamp(dt.toEpochMilli),
      ts => ts.toInstant)
  }

  final class MappedExtensionMethods(val n: Node) extends AnyVal {

    def mappedTo[U: TypedType] = Rep.forNode[U](n)

  }

  import scala.language.implicitConversions

  implicit def mappedColumnExtensions(c: Rep[_]) : MappedExtensionMethods = new MappedExtensionMethods(c.toNode)

  implicit def uuidToJava(refined: Refined[String, Uuid]): Rep[UUID] =
    UUID.fromString(refined.get).bind
}
