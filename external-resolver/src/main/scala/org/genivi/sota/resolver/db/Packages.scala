/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.types.Package

object Packages {

  import slick.driver.MySQLDriver.api._

  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def version = column[String]("version")
    def description = column[String]("description")
    def vendor = column[String]("vendor")

    def * = (id.?, name, version, description.?, vendor.?) <>
      ((Package.apply _).tupled, Package.unapply)
  }

  val packages = TableQuery[PackageTable]

  def add(pkg : Package.ValidPackage): DBIO[Int] =
    packages += pkg

  def create: DBIO[Unit] =
    packages.schema.create
}
