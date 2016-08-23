package org.genivi.sota.core.db

import java.time.Instant
import java.util.UUID

import org.genivi.sota.core.Errors
import org.genivi.sota.data.{Device, Namespace, PackageId}
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.Packages.PackageTable

case class BlacklistedPackage(id: UUID, namespace: Namespace,
                              packageId: PackageId, comment: String, updatedAt: Instant)

case class BlacklistedPackageRequest(packageId: PackageId, comment: Option[String])

object BlacklistedPackages {

  import org.genivi.sota.refined.SlickRefined._
  import org.genivi.sota.db.SlickExtensions._

  type BlacklistedPkgRow = (UUID, Namespace, PackageId.Name, PackageId.Version, String, Instant)

  private def fromTuple(row: BlacklistedPkgRow): BlacklistedPackage =
    row match {
      case (id, namespace, pkgName, pkgVersion, comment, updatedAt) =>
        BlacklistedPackage(id,
          namespace,
          PackageId(pkgName, pkgVersion),
          comment, updatedAt)
    }

  private def toTuple(v: BlacklistedPackage): Option[BlacklistedPkgRow] = Some((
    v.id,
    v.namespace,
    v.packageId.name,
    v.packageId.version,
    v.comment,
    v.updatedAt
    ))

  // scalastyle:off
  class BlacklistedPackagesTable(tag: Tag) extends Table[BlacklistedPackage](tag, "BlacklistedPackage") {
    def id = column[UUID]("uuid")
    def namespace = column[Namespace]("namespace")
    def pkgName = column[PackageId.Name]("package_name")
    def pkgVersion = column[PackageId.Version]("package_version")
    def comment = column[String]("comment")
    def active = column[Boolean]("active", O.Default(true))
    def updatedAt = column[Instant]("updated_at")

    def * = (id, namespace, pkgName, pkgVersion, comment, updatedAt) <> (fromTuple, toTuple)

    def pk = primaryKey("id", id)

    def uniquePackageId = index("BlacklistedPackage_unique_package_id", (namespace, pkgName, pkgVersion), unique = true)

    // TODO foreign key to packages?
    def packagesFk = foreignKey("BlacklistedPackage_pkg_fk", (namespace, pkgName, pkgVersion),
      TableQuery[PackageTable])(r => (r.namespace, r.name, r.version))
  }

  // scalastyle:on

  private val MissingPackageError = Errors.MissingEntity(classOf[BlacklistedPackage])

  private val all = TableQuery[BlacklistedPackagesTable]

  protected[db] val active = all.filter(_.active === true)

  def create(namespace: Namespace, pkgId: PackageId, comment: Option[String] = None)
            (implicit db: Database, ec: ExecutionContext): Future[BlacklistedPackage] = {
    val newBlacklist = BlacklistedPackage(UUID.randomUUID(), namespace,
      pkgId, comment.getOrElse(""), Instant.now())

    val dbIO = for {
      _ <- Packages.find(namespace, pkgId)
      _ <- all.insertOrUpdate(newBlacklist)
      _ <- markAsActive(namespace, pkgId)
    } yield newBlacklist

    db.run(dbIO.transactionally)
  }

  def remove(namespace: Namespace, packageId: PackageId)
            (implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val dbIO =
      findActiveQuery(namespace, packageId)
        .map(_.active)
        .update(false)
        .handleSingleUpdateError(MissingPackageError)

    db.run(dbIO)
  }

  def update(namespace: Namespace, packageId: PackageId, comment: Option[String])
            (implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val dbIO =
      findActiveQuery(namespace, packageId)
        .map(_.comment)
        .update(comment.getOrElse(""))
        .handleSingleUpdateError(MissingPackageError)

    db.run(dbIO)
  }

  def ensureNotBlacklisted(pkg: Package)(implicit ec: ExecutionContext): DBIO[Package] = {
    val isBlacklistedIO = findActiveQuery(pkg.namespace, pkg.id).exists.result

    isBlacklistedIO.flatMap {
      case false => DBIO.successful(pkg)
      case true => DBIO.failed(Errors.BlacklistedPackage)
    }
  }

  def ensureNotBlacklistedIds(namespace: Namespace)
                             (allPkgs: Seq[PackageId])
                             (implicit ec: ExecutionContext): DBIO[Seq[PackageId]] = {
    filterBlacklisted[PackageId]((namespace, _))(allPkgs).flatMap { notBlacklisted =>
      if(notBlacklisted == allPkgs)
        DBIO.successful(allPkgs)
      else
        DBIO.failed(Errors.BlacklistedPackage)
    }
  }

  def filterBlacklisted[T](pkgIdFn: T => (Namespace, PackageId))
                          (items: Seq[T])
                          (implicit ec: ExecutionContext): DBIO[Seq[T]] = {
    if(items.isEmpty)
      DBIO.successful(items)
    else {
      val namespaceBlacklist = findAction(pkgIdFn(items.head)._1)

      namespaceBlacklist.map { blacklist =>
        val blacklistedIds = blacklist.map(_.packageId)
        items.filterNot(item => blacklistedIds.contains(pkgIdFn(item)._2))
      }
    }
  }

  def isBlacklisted(namespace: Namespace, pkgId: PackageId): DBIO[Boolean] = {
    findActiveQuery(namespace, pkgId).exists.result
  }

  private def markAsActive(ns: Namespace, packageId: PackageId)
                          (implicit ec: ExecutionContext): DBIO[Unit] =
    all
      .filter(_.namespace === ns)
      .filter(_.pkgName === packageId.name)
      .filter(_.pkgVersion === packageId.version)
      .map(_.active)
      .update(true)
      .map(_ => ())

  private def findActiveQuery(ns: Namespace,
                        packageId: PackageId): Query[BlacklistedPackagesTable, BlacklistedPackage, Seq] = {
    active
      .filter(_.namespace === ns)
      .filter(_.pkgName === packageId.name)
      .filter(_.pkgVersion === packageId.version)
  }

  def findAction(namespace: Namespace): DBIO[Seq[BlacklistedPackage]] =
    active.filter(_.namespace === namespace).result

  def findFor(namespace: Namespace)(implicit db: Database): Future[Seq[BlacklistedPackage]] =
    db.run(findAction(namespace))

  def impact(namespace: Namespace)
            (implicit db: Database, ec: ExecutionContext): Future[Seq[(Device.Id, PackageId)]] = {
    val query = active
      .filter(_.namespace === namespace)
      .join(UpdateRequests.all).on { case (blacklist, requests) =>
      blacklist.namespace === requests.namespace &&
        blacklist.pkgName === requests.packageName &&
        blacklist.pkgVersion === requests.packageVersion
    }.join(UpdateSpecs.updateSpecs).on { case ((blacklist, requests), specs) =>
      requests.id === specs.requestId
    }.map { case ((blacklist, requests), specs) =>
      (specs.device, blacklist.pkgName, blacklist.pkgVersion)
    }

    val dbIO = query.result.map {
      _.map { case (deviceId, pkgName, pkgVersion) =>
        deviceId -> PackageId(pkgName, pkgVersion)
      }
    }

    db.run(dbIO)
  }
}

