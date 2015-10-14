/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}


class ComponentResourceWordSpec extends ResourceWordSpec {

  val components = "components"

  "add component on PUT /components/:partNumber { description }" in {
    addComponentOK(Refined("jobby0"), "nice")
  }

  "fail if part number isn't a 30 character or shorter alpha numeric string" in {
    addComponent(Refined(""), "bad") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
    }
    addComponent(Refined("0123456789012345678901234567890123456789"), "bad") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
    }
    addComponent(Refined("a???"), "bad") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }

  "list all available components on GET /components" in {

    addComponentOK(Refined("jobby1"), "nice")
    addComponentOK(Refined("bobby0"), "nice")
    addComponentOK(Refined("bobby1"), "nice")

    Get(Resource.uri(components)) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Component]] shouldBe List(Component(Refined("bobby0"), "nice"),
                                               Component(Refined("bobby1"), "nice"),
                                               Component(Refined("jobby0"), "nice"),
                                               Component(Refined("jobby1"), "nice"))
    }
  }

  "list all components matching a regex on GET /components?regex=:regex" in {


    Get(Resource.uri(components) + "?regex=^j.*$") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Component]] shouldBe List(Component(Refined("jobby0"), "nice"),
                                               Component(Refined("jobby1"), "nice"))
    }
    Get(Resource.uri(components) + "?regex=^.*0$") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Component]] shouldBe List(Component(Refined("bobby0"), "nice"),
                                               Component(Refined("jobby0"), "nice"))
    }
  }

  "fail on trying to list components matching a malformated regex" in {
    Get(Resource.uri(components) + "?regex=(a") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest

    }
  }

}
