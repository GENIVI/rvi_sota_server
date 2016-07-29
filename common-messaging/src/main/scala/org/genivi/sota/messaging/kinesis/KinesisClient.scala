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
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.{MessageBusPublisher, Messages}
import org.genivi.sota.messaging.Messages.{DeviceCreated, DeviceSeen, Message}
import org.genivi.sota.data.Device._
import cats.syntax.show.toShowOps
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

  def getDeviceSeenPublisher(system: ActorSystem,
                             config: Config): ConfigException Xor MessageBusPublisher[DeviceSeen] =
    getAmazonClient(system, config).map { client =>
        MessageBusPublisher { msg =>
          client.putRecord(DeviceSeen.streamName, ByteBuffer.wrap(msg.asJson.noSpaces.getBytes),
            msg.deviceId.show)
        }
    }

  def getDeviceCreatedPublisher(system: ActorSystem,
                             config: Config): ConfigException Xor MessageBusPublisher[DeviceCreated] =
  getAmazonClient(system, config).map { client =>
    MessageBusPublisher { msg =>
      client.putRecord(DeviceCreated.streamName, ByteBuffer.wrap(msg.asJson.noSpaces.getBytes),
        msg.deviceName.underlying)
    }
  }

  def runWorker(system: ActorSystem, config: Config, streamName: String, parseFn: String => io.circe.Error Xor Message)
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
        .recordProcessorFactory(new RecordProcessorFactory(system.eventStream, parseFn))
        .config(kinesisClientConfig)
        .build()

      Future(blocking { worker.run() })(system.dispatcher)
      system.registerOnTermination(Try { worker.shutdown() })
      Done
    }

  def runDeviceSeenWorker(system: ActorSystem, config: Config): ConfigException Xor Done = {
    runWorker(system, config, DeviceSeen.streamName, Messages.parseDeviceSeenMsg)
  }

  def runDeviceCreatedWorker(system: ActorSystem, config: Config): ConfigException Xor Done = {
    runWorker(system, config, DeviceCreated.streamName, Messages.parseDeviceCreatedMsg)
  }


  private implicit class StreamNameOp[T](v: T) {
    def streamName: String = {
      v.getClass.getSimpleName.filterNot(c => List('$').contains(c))
    }
  }
}
