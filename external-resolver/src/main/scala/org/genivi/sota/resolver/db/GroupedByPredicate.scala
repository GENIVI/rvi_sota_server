/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.db

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import cats.data.NonEmptyList

import scala.collection.immutable.VectorBuilder

object GroupedByPredicate {
  def apply[T, U](pred: T => U): GroupedByPredicate[T, U] = new GroupedByPredicate(pred)
}

class GroupedByPredicate[T, U](pred: T => U) extends GraphStage[FlowShape[T, NonEmptyList[T]]] {
  val in = Inlet[T]("GroupByPred.in")
  val out = Outlet[NonEmptyList[T]]("GroupByPred.out")

  override def shape: FlowShape[T, NonEmptyList[T]] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      private val buf: VectorBuilder[T] = new VectorBuilder
      private var lastPred = Option.empty[U]

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          read(in)(e => {
            val p = pred(e)

            if(lastPred.isEmpty || lastPred.contains(p)) { // same pred
              buf += e
              lastPred = Some(p)

              pull(in)
            } else { // predicate changed
              emitBuffer(() => {
                buf += e
                lastPred = Some(p)

                pull(in)
              })
            }
          }
            ,
            () => emitBuffer(completeStage)
          )
        }

        override def onUpstreamFinish(): Unit = {
          emitBuffer(completeStage)
        }
      })

      private def emitBuffer(andThen: () => Unit): Unit = {
        val b = buf.result()

        if(b.nonEmpty) {
          val e = NonEmptyList(b.head, b.tail.toList)
          buf.clear()

          emit(out, e, andThen)
        } else {
          andThen.apply()
        }
      }

      setHandler(out, new OutHandler {
        override def onPull(): Unit =
          if(!isClosed(in) && !hasBeenPulled(in)) pull(in)
      })
    }
  }
}
