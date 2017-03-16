/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.messaging.kafka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscription, Subscriptions}
import akka.stream.scaladsl.Source
import cats.data.Xor
import com.typesafe.config.{Config, ConfigException}
import io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization._
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.MessageLike

import scala.concurrent.{ExecutionContext, Future, Promise}

object KafkaClient {

  def publisher(system: ActorSystem, config: Config): Throwable Xor MessageBusPublisher =
    for {
      cfg <- config.configAt("messaging.kafka")
      topicNameFn <- topic(cfg)
      kafkaProducer <- producer(cfg)(system)
    } yield {
      new MessageBusPublisher {
        override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
          val promise = Promise[RecordMetadata]()

          val topic = topicNameFn(messageLike.streamName)

          val record = new ProducerRecord[Array[Byte], String](topic,
            messageLike.id(msg).getBytes, msg.asJson(messageLike.encoder).noSpaces)

          kafkaProducer.send(record, new Callback {
            override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
              if (exception != null)
                promise.failure(exception)
              else if (metadata != null)
                promise.success(metadata)
              else
                promise.failure(new Exception("Unknown error occurred, no metadata or error received"))
            }
          })

          promise.future.map(_ => ())
        }
      }
    }

  def source[T](system: ActorSystem, config: Config)
               (implicit ml: MessageLike[T]): Throwable Xor Source[T, NotUsed] =
    plainSource(config)(ml, system)

  private def plainSource[T](config: Config)
                            (implicit ml: MessageLike[T], system: ActorSystem): Throwable Xor Source[T, NotUsed] = {
    buildSource(config) { (cfgSettings: ConsumerSettings[Array[Byte], T], subscriptions) =>
      val settings = cfgSettings.withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
      Consumer.plainSource(settings, subscriptions).map(_.value())
    }
  }

  def committableSource[T](config: Config)
                          (implicit ml: MessageLike[T], system: ActorSystem)
  : Throwable Xor Source[CommittableMessage[Array[Byte], T], NotUsed] =
    buildSource(config) { (cfgSettings: ConsumerSettings[Array[Byte], T], subscriptions) =>
      Consumer.committableSource(cfgSettings, subscriptions)
    }

  private def consumerSettings[T](system: ActorSystem, config: Config)
                                 (implicit ml: MessageLike[T]): Throwable Xor ConsumerSettings[Array[Byte], T] =
    for {
      host <- config.readString("host")
      topicFn <- topic(config)
      groupId <- config.readString("groupIdPrefix").map(_ + "-" + topicFn(ml.streamName))
    } yield {
      ConsumerSettings(system, new ByteArrayDeserializer, new JsonDeserializer(ml.decoder))
        .withBootstrapServers(host)
        .withGroupId(groupId)
        .withClientId(s"consumer-$groupId")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    }

  private def buildSource[T, M](config: Config)
                               (consumerFn: (ConsumerSettings[Array[Byte], M], Subscription) => Source[T, Control])
                               (implicit system: ActorSystem, ml: MessageLike[M]): Throwable Xor Source[T, NotUsed] =
    for {
      cfg <- config.configAt("messaging.kafka")
      cfgSettings <- consumerSettings(system, cfg)
      topicFn <- topic(cfg)
    } yield {
      val subscription = Subscriptions.topics(topicFn(ml.streamName))
      consumerFn(cfgSettings, subscription).mapMaterializedValue { _ => NotUsed }
    }

  private[this] def topic(config: Config): ConfigException Xor (String => String) =
    config.readString("topicSuffix").map { suffix =>
      (streamName: String) => streamName + "-" + suffix
    }

  private[this] def producer(config: Config)
                            (implicit system: ActorSystem): ConfigException Xor KafkaProducer[Array[Byte], String] =
    config.readString("host").map { host =>
      ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
        .withBootstrapServers(host)
        .createKafkaProducer()
    }
}
