package org.genivi.sota.messaging.kinesis

import java.nio.ByteBuffer
import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import io.circe.syntax._
import org.genivi.sota.marshalling.CirceInstances._
import cats.data.Xor
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.typesafe.config.{Config, ConfigException}
import io.circe.{Decoder, Encoder}
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}
import org.genivi.sota.messaging.Messages._

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.Try

object KinesisClient {

  private[this] def getClientConfigWithUserAgent(appName: String, version: String): ClientConfiguration = {
    val config = new ClientConfiguration
    config.setUserAgent(s"${ClientConfiguration.DEFAULT_USER_AGENT} $appName/$version")
    config
  }

  private[this] def configureCredentialsProvider(config: Config): ConfigException Xor AWSCredentialsProvider =
    for {
      awsConfig    <- config.configAt("aws")
      awsAccessKey <- awsConfig.readString("accessKeyId")
      awsSecretKey <- awsConfig.readString("secretAccessKey")
    } yield new StaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey))

  private[this] def getAmazonClient(system: ActorSystem, config: Config): ConfigException Xor AmazonKinesisClient =
    for {
      cfg          <- config.configAt("messaging.kinesis")
      appName      <- cfg.readString("appName")
      regionName   <- cfg.readString("regionName")
      region       =  Regions.fromName(regionName)
      version      <- cfg.readString("appVersion")
      clientConfig = getClientConfigWithUserAgent(appName, version)
      credentials  <- configureCredentialsProvider(config)
    } yield {
      val client = new AmazonKinesisClient(credentials, clientConfig)
      client.configureRegion(region)
      system.registerOnTermination(client.shutdown())
      client
    }

  def publisher(system: ActorSystem, config: Config): ConfigException Xor MessageBusPublisher =
    getAmazonClient(system, config).map { client =>
      new MessageBusPublisher {
        override def publish[T <: Message](msg: T)(implicit ex: ExecutionContext, encoder: Encoder[T]): Future[Unit] =
          Future {
            blocking {
              client.putRecord(msg.streamName, ByteBuffer.wrap(msg.asJson.noSpaces.getBytes), msg.partitionKey)
            }
          }
      }
    }

  def source[T <: Message](system: ActorSystem, config: Config, streamName: String)
                          (implicit decoder: Decoder[T])
                             : ConfigException Xor Source[T, NotUsed] =
    for {
      cfg                 <- config.configAt("messaging.kinesis")
      appName             <- cfg.readString("appName")
      regionName          <- cfg.readString("regionName")
      version             <- cfg.readString("appVersion")
      clientConfig        = getClientConfigWithUserAgent(appName, version)
      credentials         <- configureCredentialsProvider(config)
      kinesisClientConfig = new KinesisClientLibConfiguration(
        appName,
        streamName,
        credentials,
        UUID.randomUUID().toString).withRegionName(regionName).withCommonClientConfig(clientConfig)
    } yield {
      Source.actorRef[T](MessageBus.DEFAULT_CLIENT_BUFFER_SIZE,
        OverflowStrategy.dropTail).mapMaterializedValue { ref =>

        implicit val ec = system.dispatcher
        implicit val _system = system

        val worker = new Worker.Builder()
          .recordProcessorFactory(new RecordProcessorFactory(ref))
          .config(kinesisClientConfig)
          .build()

        Future(blocking { worker.run() })
        system.registerOnTermination(Try { worker.shutdown() })

        NotUsed
      }
    }
}
