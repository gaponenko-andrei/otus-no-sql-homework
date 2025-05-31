package demo

import cats.effect.{IO, IOApp}
import fs2.kafka._
import io.circe.syntax._
import java.util.UUID
import java.time.Instant

object ProducerDemoApp extends IOApp.Simple {

  val producerSettings: ProducerSettings[IO, String, String] =
    ProducerSettings[IO, String, String]
      .withBootstrapServers("localhost:9094")

  val geoEvent: GeoEvent = GeoEvent(
    eventId = UUID.randomUUID(),
    userId = UUID.randomUUID(),
    deviceId = UUID.randomUUID(),
    speed = 42,
    system = GeoEvent.OS.Android,
    point = GeoEvent.GeoPoint(55.75, 37.61),
    timestamp = Instant.now()
  )

  val record: ProducerRecord[String, String] =
    ProducerRecord.apply(
      topic = "geo-event",
      key = geoEvent.eventId.toString,
      value = geoEvent.asJson.noSpaces
    )

  override def run: IO[Unit] =
    KafkaProducer
      .resource(producerSettings)
      .use(_.produce(ProducerRecords.one(record)).flatten)
      .void
}