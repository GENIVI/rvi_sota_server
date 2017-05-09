/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import java.util.UUID

import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.db.Package.Metadata
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

/**
 * Data access object for Packages
 */
object PackageRepository {

  import org.genivi.sota.db.SlickExtensions._

  /**
   * DAO Mapping Class for the Package table in the database
   */
  // scalastyle:off
  private[db] class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def uuid        = column[UUID]("uuid")
    def namespace   = column[Namespace]("namespace")
    def name        = column[PackageId.Name]("name")
    def version     = column[PackageId.Version]("version")
    def description = column[String]("description")
    def vendor      = column[String]("vendor")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_package", uuid)

    def * = (uuid, namespace, name, version, description.?, vendor.?).shaped <>
      (pkg => Package(pkg._1, pkg._2, PackageId(pkg._3, pkg._4), pkg._5, pkg._6),
        (pkg: Package) => Some((pkg.uuid, pkg.namespace, pkg.id.name, pkg.id.version, pkg.description, pkg.vendor)))
  }
  // scalastyle:on

  protected[db] val packages = TableQuery[PackageTable]

  def add(id: PackageId, metadata: Metadata)(implicit ec: ExecutionContext): DBIO[Package] = {
    def findById(packageId: PackageId): DBIO[Option[Package]] =
      packages
        .filter(_.namespace === metadata.namespace).filter(_.name === id.name).filter(_.version === id.version)
        .result.headOption

    val pkg = Package(UUID.randomUUID(), metadata.namespace, id, metadata.description, metadata.vendor)

    val dbio = for {
      maybeExisting <- findById(id)
      newPkg = maybeExisting match {
        case Some(existing) => pkg.copy(uuid = existing.uuid)
        case None => pkg
      }
      _ <- packages.insertOrUpdate(newPkg)
    } yield pkg

    dbio.transactionally
  }

  /**
   * Lists the packages in the Package table
 *
   * @return     A DBIO[Seq[Package]] for the packages in the table
   */
  def list(namespace: Namespace): DBIO[Seq[Package]] =
    packages.filter(_.namespace === namespace).result

  /**
   * Checks to see if a package exists in the database
 *
   * @param pkgId   The Id of the package to check for
   * @return        The DBIO[Package] if the package exists
   */
  def exists(namespace: Namespace, pkgId: PackageId)(implicit ec: ExecutionContext): DBIO[Package] =
    packages
      .filter(p => p.namespace === namespace
                && p.name      === pkgId.name
                && p.version   === pkgId.version)
      .result
      .headOption
      .failIfNone(Errors.MissingPackage)

  /**
   * Loads a group of Packages from the database by ID
 *
   * @param ids     A Set[Package.Id] of Ids to load
   * @return        A DBIO[Set[Package]] of matched packages
   */
  def load(namespace: Namespace, ids: Set[PackageId])
          (implicit ec: ExecutionContext): DBIO[Set[Package]] = {
    packages.filter( x =>
      x.namespace === namespace &&
      (x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map(id => id.name.get + id.version.get))
    ).result.map( _.toSet )
  }

  def loadByUuids(uuids: Set[UUID]): DBIO[Seq[Package]] = {
    packages.filter(_.uuid.inSet(uuids)).result
  }

  protected[db] def toPackageUuids(namespace: Namespace, ids: Set[PackageId])
                                  (implicit ec: ExecutionContext): DBIO[Set[UUID]] = {
    inSetQuery(ids)
      .filter(_.namespace === namespace)
      .map(_.uuid)
      .result
      .map(_.toSet)
  }

  protected[db] def inSetQuery(ids: Set[PackageId]): Query[PackageTable, Package, Seq] = {
    packages.filter { pkg =>
      (pkg.name.mappedTo[String] ++ pkg.version.mappedTo[String]).inSet(ids.map(id => id.name.get + id.version.get))
    }
  }

  protected[db] def withNameQuery(ns: Namespace, name: PackageId.Name): Query[PackageTable, Package, Seq] = {
    packages.filter { pkg => pkg.namespace === ns && pkg.name === name }
  }
}
