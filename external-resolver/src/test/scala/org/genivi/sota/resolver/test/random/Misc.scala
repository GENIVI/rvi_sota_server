package org.genivi.sota.resolver.test.random

import cats.state.StateT
import cats.{Monad, Comonad}
import org.scalacheck.Gen


object Misc {

  implicit val monGen: Monad[Gen] = new Monad[Gen] {

    def pure[X](x: X): Gen[X] =
      Gen.const(x)

    def flatMap[X, Y](gx: Gen[X])(k: X => Gen[Y]): Gen[Y] =
      gx.flatMap(k)

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
    }

}
