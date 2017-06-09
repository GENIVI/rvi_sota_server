package org.genivi.sota.resolver.test.random

import cats.data.StateT
import cats.{Comonad, Monad}
import org.scalacheck.Gen

import scala.annotation.tailrec


object Misc {

  implicit val monGen: Monad[Gen] = new Monad[Gen] {

    def pure[X](x: X): Gen[X] =
      Gen.const(x)

    def flatMap[X, Y](gx: Gen[X])(k: X => Gen[Y]): Gen[Y] =
      gx.flatMap(k)

    override def tailRecM[A, B](a: A)(f: (A) => Gen[Either[A, B]]): Gen[B] =
      f(a).flatMap {
        case Left(ex) => tailRecM(ex)(f)
        case Right(r) => pure(r)
      }
  }

  def lift[X, S](gen: Gen[X]): StateT[Gen, S, X] =
    StateT(s => gen.flatMap(x => (s, x)))

  // How to import this from cats?
  implicit val function0Instance: Comonad[Function0] with Monad[Function0] =
    new Comonad[Function0] with Monad[Function0] {
      def extract[A](x: () => A): A = x()

      def coflatMap[A, B](fa: () => A)(f: (() => A) => B): () => B =
        () => f(fa)

      def pure[A](x: A): () => A = () => x

      def flatMap[A, B](fa: () => A)(f: A => () => B): () => B =
        () => f(fa())()

      override def tailRecM[A, B](a: A)(f: (A) => () => Either[A, B]): () => B = () => {
        @tailrec
        def go(x: A): B = f(x)() match {
          case Left(ex) => go(ex)
          case Right(r) => r
        }
        go(a)
      }
    }


}
