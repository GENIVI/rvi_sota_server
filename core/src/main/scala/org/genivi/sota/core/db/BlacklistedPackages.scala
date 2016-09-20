package org.genivi.sota.core.db

import java.time.Instant
import java.util.UUID

import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.data.{Device, Namespace, PackageId}
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.data.client.GenericResponseEncoder
import org.genivi.sota.core.db.Packages.{LiftedPackageId, PackageTable}
import org.genivi.sota.http.Errors
import org.genivi.sota.http.Errors.MissingEntity

case class BlacklistedPackage(id: UUID, namespace: Namespace,
                              packageId: PackageId, comment: String, updatedAt: Instant)

case class BlacklistedPackageRequest(packageId: PackageId, comment: Option[String])

case class BlacklistedPackageResponse(packageId: PackageId, comment: String, updatedAt: Instant)

object BlacklistedPackageResponse {
  implicit val toResponse = GenericResponseEncoder { (blackList: BlacklistedPackage) =>
    BlacklistedPackageResponse(blackList.packageId, blackList.comment, blackList.updatedAt)
  }
}


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
  }

  // scalastyle:on



  private val all = TableQuery[BlacklistedPackagesTable]

  protected[db] val active = all.filter(_.active === true)

  private val BlacklistExists = Errors.EntityAlreadyExists(classOf[BlacklistedPackage])
  private val MissingBlacklistError = MissingEntity(classOf[BlacklistedPackage])

  def create(namespace: Namespace, pkgId: PackageId, comment: Option[String] = None)
            (implicit db: Database, ec: ExecutionContext): Future[BlacklistedPackage] = {
    val newBlacklist = BlacklistedPackage(UUID.randomUUID(), namespace,
      pkgId, comment.getOrElse(""), Instant.now())

    def insertOrUpdate(): DBIO[Unit] = {
      val findQuery = all
        .filter(_.namespace === namespace)
        .filter(_.pkgName === pkgId.name)
        .filter(_.pkgVersion === pkgId.version)

      findQuery.map(_.active).result.headOption.flatMap { existing =>
        val isActive = existing.contains(true)

        if(isActive)
          DBIO.failed(BlacklistExists)
        else if (existing.isDefined)
          findQuery.map(b => (b.active, b.comment))
            .update((true, newBlacklist.comment))
            .handleSingleUpdateError(MissingBlacklistError)
        else
          (all += newBlacklist)
            .andThen(markAsActive(namespace, pkgId))
            .handleIntegrityErrors(BlacklistExists)
      }
    }

    val dbIO = insertOrUpdate().map(_ => newBlacklist)

    db.run(dbIO.transactionally)
  }

  def remove(namespace: Namespace, packageId: PackageId)
            (implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val dbIO =
      findActiveQuery(namespace, packageId)
        .map(_.active)
        .update(false)
        .handleSingleUpdateError(MissingBlacklistError)

    db.run(dbIO)
  }

  def update(namespace: Namespace, packageId: PackageId, comment: Option[String])
            (implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val dbIO =
      findActiveQuery(namespace, packageId)
        .map(_.comment)
        .update(comment.getOrElse(""))
        .handleSingleUpdateError(MissingBlacklistError)

    db.run(dbIO)
  }

  def ensureNotBlacklisted(pkg: Package)(implicit ec: ExecutionContext): DBIO[Package] = {
    val isBlacklistedIO = findActiveQuery(pkg.namespace, pkg.id).exists.result

    isBlacklistedIO.flatMap {
      case false => DBIO.successful(pkg)
      case true => DBIO.failed(SotaCoreErrors.BlacklistedPackage)
    }
  }

  def ensureNotBlacklistedIds(namespace: Namespace)
                             (allPkgs: Seq[PackageId])
                             (implicit ec: ExecutionContext): DBIO[Seq[PackageId]] =
    filterBlacklisted[PackageId]((namespace, _))(allPkgs).flatMap { notBlacklisted =>
      if(notBlacklisted == allPkgs) {
        DBIO.successful(allPkgs)
      } else {
        DBIO.failed(SotaCoreErrors.BlacklistedPackage)
      }
    }

  def filterBlacklisted[T](pkgIdFn: T => (Namespace, PackageId))
                          (items: Seq[T])
                          (implicit ec: ExecutionContext): DBIO[Seq[T]] =
    if (items.isEmpty) {
      DBIO.successful(items)
    } else {
      val namespaceBlacklist = findAction(pkgIdFn(items.head)._1)

      namespaceBlacklist.map { blacklist =>
        val blacklistedIds = blacklist.map(_.packageId)
        items.filterNot(item => blacklistedIds.contains(pkgIdFn(item)._2))
      }
    }

  def isBlacklisted(namespace: Namespace, pkgId: PackageId): DBIO[Boolean] =
    findActiveQuery(namespace, pkgId).exists.result

  private def markAsActive(namespace: Namespace, packageId: PackageId)
                          (implicit ec: ExecutionContext): DBIO[Unit] =
    all
      .filter(_.namespace === namespace)
      .filter(_.pkgName === packageId.name)
      .filter(_.pkgVersion === packageId.version)
      .map(_.active)
      .update(true)
      .map(_ => ())

  private def findActiveQuery(namespace: Namespace,
                              packageId: PackageId): Query[BlacklistedPackagesTable, BlacklistedPackage, Seq] =
    active
      .filter(_.namespace === namespace)
      .filter(_.pkgName === packageId.name)
      .filter(_.pkgVersion === packageId.version)

  private def findAction(namespace: Namespace): DBIO[Seq[BlacklistedPackage]] =
    active.filter(_.namespace === namespace).result

  def findFor(namespace: Namespace)(implicit db: Database): Future[Seq[BlacklistedPackage]] =
    db.run(findAction(namespace))

  def impact(namespace: Namespace, impactedDevicesFn: Set[PackageId] => Future[Map[Device.Id, Seq[PackageId]]])
            (implicit db: Database, ec: ExecutionContext): Future[Map[Device.Id, Seq[PackageId]]] = {
    val query =  active.filter(_.namespace === namespace).map(r => LiftedPackageId(r.pkgName, r.pkgVersion)).result
    db.run(query).map(_.toSet).flatMap(impactedDevicesFn)
  }
}
