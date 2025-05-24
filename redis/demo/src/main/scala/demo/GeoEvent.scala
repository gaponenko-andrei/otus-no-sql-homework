package demo

import cats.syntax.apply._
import demo.GeoEvent._
import demo.GenCatsInstances._
import org.scalacheck.Gen

import java.time.Instant
import java.util.UUID

import io.circe._
import io.circe.generic.semiauto._

case class GeoEvent(
  eventId: UUID,
  userId: UUID,
  deviceId: UUID,
  speed: Int,
  system: OS,
  point: GeoPoint,
  timestamp: Instant
)

object GeoEvent {
  implicit val codec: Codec[GeoEvent] = deriveCodec

  implicit val gen: Gen[GeoEvent] =
    (
      Gen.uuid,
      Gen.uuid,
      Gen.uuid,
      Gen.choose(0, 400),
      OS.gen,
      GeoPoint.gen,
      Gen.choose(0L, Instant.now().getEpochSecond).map(Instant.ofEpochSecond)
    ).mapN(GeoEvent.apply)

  // GeoPoint

  case class GeoPoint(lat: Double, lon: Double)
  object GeoPoint {
    implicit val codec: Codec[GeoPoint] = deriveCodec

    implicit val gen: Gen[GeoPoint] =
      (
        Gen.choose(-90.0, 90.0),
        Gen.choose(-180.0, 180.0)
      ).mapN(GeoPoint.apply)
  }

  // OS

  sealed trait OS extends Product with Serializable
  object OS {
    implicit val codec: Codec[OS] = deriveCodec
    implicit val gen: Gen[OS] = Gen.oneOf(Android, iOS)

    case object Android extends OS
    case object iOS extends OS
  }
}