/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.db

import akka.http.scaladsl.model.Uri
import java.util.UUID
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

  final class MappedExtensionMethods(val n: Node) extends AnyVal {

    def mappedTo[U: TypedType] = Rep.forNode[U](n)

  }

  import scala.language.implicitConversions

  implicit def mappedColumnExtensions(c: Rep[_]) : MappedExtensionMethods = new MappedExtensionMethods(c.toNode)
}
