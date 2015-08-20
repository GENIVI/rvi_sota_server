package org.genivi.sota.resolver.test


class ResolveResourceWordSpec extends ResourceWordSpec {

  "Resolve resource" should {

    "give back a list of vehicles onto which the supplied package should be installed" in {

      // Add some vehicles.
      val vins = List(
        "00RESOLVEVIN12345",
        "01RESOLVEVIN12345",
        "10RESOLVEVIN12345",
        "11RESOLVEVIN12345")

      vins map addVehicleOK

      // Add a package.
      addPackageOK("resolve pkg", "0.0.1", None, None)

      // Add a trival filter that lets all vins through.
      addFilterOK("truefilter", "TRUE")
      addPackageFilterOK("resolve pkg", "0.0.1", "truefilter")

      resolveOK("resolve pkg", "0.0.1", vins)

      // Add another filter.
      addFilterOK("0xfilter", s"""vin_matches "^00.*" OR vin_matches "^01.*"""")
      addPackageFilterOK("resolve pkg", "0.0.1", "0xfilter")

      resolveOK("resolve pkg", "0.0.1",
        List("00RESOLVEVIN12345", "01RESOLVEVIN12345"))

      // Add trivially false filter.
      addFilterOK("falsefilter", "FALSE")
      addPackageFilterOK("resolve pkg", "0.0.1", "falsefilter")

      resolveOK("resolve pkg", "0.0.1", List())

    }
  }

}
