package org.genivi.sota.refined

import org.genivi.sota.data.PackageId
import slick.lifted.{CaseClassShape, Rep}
import slick.jdbc.MySQLProfile.api._
import org.genivi.sota.refined.SlickRefined._

object PackageIdDatabaseConversions {


  case class LiftedPackageId(name: Rep[PackageId.Name], version: Rep[PackageId.Version])

  implicit object LiftedPackageShape extends CaseClassShape(LiftedPackageId.tupled,
    (p: (PackageId.Name, PackageId.Version)) => PackageId(p._1, p._2))

}

