package org.genivi.sota.messaging.kinesis

import java.nio.ByteBuffer
import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
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
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages._

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.blocking

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

  def getPublisher[T <: Message](system: ActorSystem, config: Config)
                                (implicit encoder: Encoder[T]): ConfigException Xor MessageBusPublisher[T] =
    getAmazonClient(system, config).map { client =>
      MessageBusPublisher { msg =>
        client.putRecord(msg.streamName, ByteBuffer.wrap(msg.asJson.noSpaces.getBytes),
          msg.partitionKey)
      }
    }

  def runWorker[T <: Message](system: ActorSystem, config: Config, streamName: String)
                             (implicit decoder: Decoder[T])
                             : ConfigException Xor Done =
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
      val worker = new Worker.Builder()
        .recordProcessorFactory(new RecordProcessorFactory(system.eventStream))
        .config(kinesisClientConfig)
        .build()

      Future(blocking { worker.run() })(system.dispatcher)
      system.registerOnTermination(Try { worker.shutdown() })
      Done
    }
}
