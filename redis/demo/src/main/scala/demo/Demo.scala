package demo

import io.circe.syntax.EncoderOps
import redis.clients.jedis.Jedis
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Random

object Demo extends App {
  val TargetSizeMb = 20
  val TargetSizeBytes = TargetSizeMb * 1024 * 1024
  val ReadTestSize = 10000

  val jedis = new Jedis("localhost", 6379)

  try {

    // ========= Генерация =========

    println("Генерируем данные на ~20МБ")
    println("-" * 33)

    val events = genEvents(TargetSizeBytes)
    println(s" * Сгенерировано ${events.size} событий")
    val avgJsonSize = events.map(jsonBytesSize).sum / events.size
    println(s" * Средний размер json одной записи: $avgJsonSize байт")

    // ========= Тестирование =========

    testJsonWrite(events)
    testJsonRead()

    testHsetWrite(events)
    testHsetRead()

    testZsetWrite(events)
    testZsetRead()

    testListWrite(events)
    testListRead()

  } finally {
    jedis.flushDB()
    jedis.close()
  }

  // ========= Методы тестирования записи =========

  def testJsonWrite(events: List[GeoEvent]): Unit = {
    println("\nТестирование записи JSON-строк ...")
    println("-" * 33)

    jedis.flushDB()
    val timings = events.map { event =>
      val key = s"GeoEvent:json:${event.eventId}"
      val json = event.asJson.noSpaces
      countNanos(jedis.set(key, json))
    }

    printResults(events.size, timings)
  }

  def testHsetWrite(events: List[GeoEvent]): Unit = {
    println("\nТестирование записи HSET ...")
    println("-" * 33)

    jedis.flushDB()
    val timings = events.map { event =>
      val key = s"GeoEvent:hset:${event.eventId}"
      val fields = Map(
        "eventId" -> event.eventId.toString,
        "userId" -> event.userId.toString,
        "deviceId" -> event.deviceId.toString,
        "speed" -> event.speed.toString,
        "system" -> event.system.toString,
        "lat" -> event.point.lat.toString,
        "lon" -> event.point.lon.toString,
        "timestamp" -> event.timestamp.toString
      ).asJava
      countNanos(jedis.hset(key, fields))
    }

    printResults(events.size, timings)
  }

  def testZsetWrite(events: List[GeoEvent]): Unit = {
    println("\nТестирование записи ZSET ...")
    println("-" * 33)

    jedis.flushDB()
    val timings = events.map { event =>
      val key = "GeoEvent:zset:byTimestamp"
      val score = event.timestamp.getEpochSecond.toDouble
      val member = event.asJson.noSpaces
      countNanos(jedis.zadd(key, score, member))
    }

    printResults(events.size, timings)
  }

  def testListWrite(events: List[GeoEvent]): Unit = {
    println("\nТестирование записи List (LPUSH) ...")
    println("-" * 33)

    jedis.flushDB()
    val timings = events.map { event =>
      val key = "GeoEvent:list:all"
      val value = event.asJson.noSpaces
      countNanos(jedis.lpush(key, value))
    }

    printResults(events.size, timings)
  }

  // ========= Методы тестирования чтения =========

  def testJsonRead(): Unit = {
    println("\nТестирование чтения JSON ...")
    println("-" * 33)

    val keys = Random.shuffle(testKeys("GeoEvent:json:*"))
    val timings = keys.map(key => countNanos(jedis.get(key)))

    printResults(ReadTestSize, timings)
  }

  def testHsetRead(): Unit = {
    println("\nТестирование чтения HSET ...")
    println("-" * 33)

    val keys = testKeys("GeoEvent:hset:*")
    val timings = keys.map(key => countNanos(jedis.hgetAll(key)))

    printResults(ReadTestSize, timings)
  }

  def testZsetRead(): Unit = {
    println("\nТестирование чтения ZSET ...")
    println("-" * 33)

    val timings = (1 to ReadTestSize).map { _ =>
      countNanos {
        val randScore = System.currentTimeMillis() / 1000.0 - Random.nextInt(100000)
        jedis.zrangeByScoreWithScores("GeoEvent:zset:byTimestamp", randScore, randScore + 1000)
      }
    }.toList

    printResults(ReadTestSize, timings)
  }

  def testListRead(): Unit = {
    println("\nТестирование чтения List (последние элементы) ...")
    println("-" * 33)

    val listKey = "GeoEvent:list:all"
    val listLength = jedis.llen(listKey).toInt
    val batchSize = 10

    val timings = (1 to ReadTestSize).map { _ =>
      val start = -batchSize.min(listLength)
      countNanos(jedis.lrange(listKey, start, -1))
    }.toList

    printResults( ReadTestSize, timings)
  }

  def testKeys(pattern: String) =
    jedis
      .keys(pattern)
      .asScala
      .toList
      .take(ReadTestSize)

  // ========= Вспомогательные методы =========

  def countNanos[T](op: => T): Long = {
    val start = System.nanoTime()
    val _ = op
    System.nanoTime() - start
  }

  def printResults(count: Int, timings: List[Long]): Unit = {
    val avgTimeNanos = timings.sum / count
    val totalTimeSeconds = timings.sum.toDouble / 1_000_000_000

    println(s" * Общее время: ${"%.3f".format(totalTimeSeconds)} сек")
    println(s" * Среднее время: $avgTimeNanos нс")
    println(s" * Скорость: ${"%.2f".format(count / totalTimeSeconds)} опер/сек")
  }

  @tailrec
  def genEvents(
    targetSizeBytes: Long,
    acc: List[GeoEvent] = Nil,
    currentSize: Long = 0
  ): List[GeoEvent] =
    if (currentSize >= targetSizeBytes) acc
    else {
      val event = GeoEvent.gen.sample.get
      val eventSize = jsonBytesSize(event)
      val totalSize = currentSize + eventSize
      genEvents(targetSizeBytes, event :: acc, totalSize)
    }

  def jsonBytesSize(event: GeoEvent): Int =
    event.asJson.noSpaces.getBytes("UTF-8").length
}