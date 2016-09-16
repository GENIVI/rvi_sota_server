/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.genivi.sota.resolver.db.GroupedByPredicate
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest._

class GroupedByPredicateSpec(_system: ActorSystem) extends TestKit(_system)
  with PropSpecLike with PropertyChecks with ShouldMatchers {

  def this() = this(ActorSystem("GroupedByPredicateSpec"))

  implicit val mat = ActorMaterializer()

  property("empty stream does not emit any elements") {
    val s = Source.empty[Int]
    val grouped = GroupedByPredicate[Int, Int](identity)

    val stream = s.via(grouped).runWith(TestSink.probe)

    stream.expectSubscriptionAndComplete()
  }

  property("groups values by predicate") {
    val grouped = GroupedByPredicate[Int, Boolean](_ % 2 == 0)
    val source = Source(List(1, 3, 5, 2, 4, 6))

    val stream = source.via(grouped).runWith(TestSink.probe)

    val first = stream.requestNext()

    first.head shouldBe 1
    first.tail shouldBe List(3, 5)

    val second = stream.requestNext()

    second.head shouldBe 2
    second.tail shouldBe List(4, 6)

    stream.expectComplete()
  }

  property("groups values by predicate with 0") {
    val grouped = GroupedByPredicate[Int, Boolean](_ % 2 == 0)
    val source = Source(List(0, 1))

    val stream = source.via(grouped).runWith(TestSink.probe)

    val first = stream.requestNext()

    first.head shouldBe 0
    first.tail shouldBe List()

    val second = stream.requestNext()

    second.head shouldBe 1
    second.tail shouldBe List()

    stream.expectComplete()
  }

  property("groups lists of 1 element into one element") {
    val oneElem: Gen[List[Int]] = Gen.listOfN(1, Gen.posNum[Int])

    forAll(oneElem) { l0 =>
      val grouped = GroupedByPredicate[Int, Boolean](_ => true)
      val source = Source(l0)

      val stream = source.via(grouped).runWith(TestSink.probe)

      val first = stream.requestNext()

      first.head shouldBe l0.head
      first.tail shouldBe l0.tail
    }
  }

  property("groups by predicate with lists bigger than 2 distinct elements") {
    forAll { (l: List[Int]) =>
      whenever(l.distinct.size > 2) {
        val (l0, l1) = l.filter(_ != 0).partition(_ % 2 == 0)

        val grouped = GroupedByPredicate[Int, Boolean](l0.contains(_))
        val source = Source(l0 ++ l1)

        val stream = source.via(grouped).runWith(TestSink.probe)

        if (l0.nonEmpty) {
          val first = stream.requestNext()

          first.head shouldBe l0.head
          first.tail shouldBe l0.tail
        }

        if(l1.nonEmpty) {
          val second = stream.requestNext()

          second.head shouldBe l1.head
          second.tail shouldBe l1.tail
        }

        stream.expectComplete()
      }
    }
  }
}
