package demo

import cats.effect.{IO, IOApp}
import fs2.kafka._
import io.circe.parser.decode

object ConsumerDemoApp extends IOApp.Simple {

  val consumerSettings: ConsumerSettings[IO, String, String] =
    ConsumerSettings[IO, String, String]
      .withGroupId("demo.consumer.group")
      .withBootstrapServers("localhost:9094")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)

  override def run: IO[Unit] =
    KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo("geo-event")
      .records
      .evalMap(print)
      .compile
      .drain

  def print(committable: CommittableConsumerRecord[IO, String, String]): IO[Unit] = {
    val value = committable.record.value

    decode[GeoEvent](value) match {
      case Right(event) => IO.println(s"Received: $event")
      case Left(err) => IO.println(s"Failed to decode: $err\nRaw: $value")
    }
  }
}