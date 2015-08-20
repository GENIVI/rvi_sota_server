package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import eu.timepit.refined.Refined
import org.genivi.sota.refined.SprayJsonRefined._
import org.genivi.sota.resolver.db.Resolve.makeFakeDependencyMap
import org.genivi.sota.resolver.types.Package
import org.genivi.sota.resolver.types.Package.Metadata
import org.genivi.sota.resolver.types.{Vehicle, Filter, PackageFilter}
import spray.json.DefaultJsonProtocol._


class ResolveResourceWordSpec extends ResourceWordSpec {

  "Resolve resource" should {

    "give back a list of vehicles onto which the supplied package should be installed" in {

      // Add some vehicles.
      Put(VehiclesUri("00RESOLVEVIN12345")) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
      Put(VehiclesUri("01RESOLVEVIN12345")) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
      Put(VehiclesUri("10RESOLVEVIN12345")) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
      Put(VehiclesUri("11RESOLVEVIN12345")) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }

      // Add a package.
      addPackage("resolve pkg", "0.0.1", None, None) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      // Add a trival filter that lets all vins through.

      def addFilter(name: String, expr: String): Unit =
        Post(FiltersUri, Filter(Refined(name), Refined(expr))) ~> route ~> check {
          status shouldBe StatusCodes.OK
        }

      addFilter("truefilter", "TRUE")

      // Associate filters to package.

      def assocFilterToPackage(pname: String, pversion: String, fname: String): Unit =
        Post(PackageFiltersUri, PackageFilter(Refined(pname), Refined(pversion), Refined(fname))) ~> route ~> check {
          status shouldBe StatusCodes.OK
        }

      assocFilterToPackage("resolve pkg", "0.0.1", "truefilter")

      def resolve(pname: String, pversion: String, vins: Seq[String]): Unit =
        Get(ResolveUri(pname, pversion)) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Map[Vehicle.Vin, List[Package.Id]]] shouldBe
            makeFakeDependencyMap(Refined(pname), Refined(pversion), vins.map(s => Vehicle(Refined(s))))
        }

      resolve("resolve pkg", "0.0.1",
        List("00RESOLVEVIN12345", "01RESOLVEVIN12345", "10RESOLVEVIN12345", "11RESOLVEVIN12345"))

      // Add another filter.
      addFilter("0xfilter", s"""vin_matches "^00.*" OR vin_matches "^01.*"""")
      assocFilterToPackage("resolve pkg", "0.0.1", "0xfilter")

      resolve("resolve pkg", "0.0.1",
        List("00RESOLVEVIN12345", "01RESOLVEVIN12345"))

      // Add trivially false filter.
      addFilter("falsefilter", "FALSE")
      assocFilterToPackage("resolve pkg", "0.0.1", "falsefilter")

      resolve("resolve pkg", "0.0.1", List())

    }
  }

}
