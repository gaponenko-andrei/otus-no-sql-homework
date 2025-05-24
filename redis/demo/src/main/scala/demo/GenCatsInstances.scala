package demo

import cats.{Functor, Semigroupal}
import org.scalacheck.Gen

object GenCatsInstances {

  implicit val genFunctor: Functor[Gen] = new Functor[Gen] {
    def map[A, B](fa: Gen[A])(f: A => B): Gen[B] = fa.map(f)
  }

  implicit val genSemigroupal: Semigroupal[Gen] = new Semigroupal[Gen] {
    def product[A, B](fa: Gen[A], fb: Gen[B]): Gen[(A, B)] = Gen.zip(fa, fb)
  }
}
