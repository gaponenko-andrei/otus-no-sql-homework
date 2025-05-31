package demo

import demo.GeoEvent._
import io.circe._
import io.circe.generic.semiauto._

import java.time.Instant
import java.util.UUID

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

  case class GeoPoint(lat: Double, lon: Double)
  object GeoPoint {
    implicit val codec: Codec[GeoPoint] = deriveCodec
  }

  sealed trait OS extends Product with Serializable
  object OS {
    implicit val codec: Codec[OS] = deriveCodec

    case object Android extends OS
    case object iOS extends OS
  }
}