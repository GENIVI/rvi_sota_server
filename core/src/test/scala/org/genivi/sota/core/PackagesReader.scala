package org.genivi.sota.core

import akka.http.scaladsl.model.Uri
import eu.timepit.refined.Refined
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.data.PackageId


object PackagesReader {

  private[this] def readPackage( src: Map[String, String] ) : Package = {
    if( src.get( "Package").isEmpty ) println( src )
    val maybePackage = for {
      name        <- src.get( "Package" )
      version     <- src.get( "Version" )
      size        <- src.get("Size").map( _.toLong )
      checkSum    <- src.get("SHA1")
    } yield Package( PackageId( Refined(name), Refined(version)), size = size, description = src.get( "Description" ), checkSum = checkSum, uri = Uri.Empty, vendor = src.get( "Maintainer" ) )
    maybePackage.get
  }

  def read() = {
    val src = scala.io.Source.fromInputStream( this.getClass().getResourceAsStream("/Packages") )
    src.getLines().foldLeft( List(Map.empty[String, String]) ){ (acc, str) =>
      if( str.startsWith(" ") ) acc
      else {
        if( str.isEmpty ) Map.empty[String, String] :: acc
        else {
          val keyValue = str.split(": ").toList
          acc.head.updated(keyValue.head, keyValue.tail.mkString(": ")) :: acc.tail
        }
      }
    }.filter(_.nonEmpty).map(readPackage)
  }
}
