package org.genivi.sota.messaging.kinesis

import java.nio.ByteBuffer
import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorSystem, Status}
import akka.actor.FSM.Failure
import akka.event.Logging
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Source}
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
import org.genivi.sota.messaging.Messages.MessageLike
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.{Success, Try}
import akka.pattern.pipe
import org.apache.commons.logging.LogFactory

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

  private[this] def getStreamNameSuffix(streamNameRoot: String, config: Config): ConfigException Xor String =
    for {
      awsConfig    <- config.configAt("aws")
      streamNameSuffix <- config.readString("streamNameSuffix")
    } yield streamNameRoot + "-" + streamNameSuffix

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
        override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] =
          Future {
            blocking {
              for {
                streamName <- getStreamNameSuffix(messageLike.streamName, config)
                _          = client.putRecord(
                              streamName,
                              ByteBuffer.wrap(msg.asJson(messageLike.encoder).noSpaces.getBytes),
                              messageLike.partitionKey(msg))
              } yield {}
            }
          }
      }
    }

  def source[T](system: ActorSystem, config: Config, streamNameRoot: String)(implicit decoder: Decoder[T])
                             : ConfigException Xor Source[T, NotUsed] =
    for {
      cfg                 <- config.configAt("messaging.kinesis")
      appName             <- cfg.readString("appName")
      regionName          <- cfg.readString("regionName")
      version             <- cfg.readString("appVersion")
      streamName          <- getStreamNameSuffix(streamNameRoot, config)
      clientConfig        = getClientConfigWithUserAgent(appName, version)
      credentials         <- configureCredentialsProvider(config)
      kinesisClientConfig = new KinesisClientLibConfiguration(
        appName,
        streamName,
        credentials,
        s"$streamName-worker-" + UUID.randomUUID().toString)
        .withRegionName(regionName).withCommonClientConfig(clientConfig)
    } yield {
      Source.actorRef[T](MessageBus.DEFAULT_CLIENT_BUFFER_SIZE,
        OverflowStrategy.dropTail).mapMaterializedValue { ref =>

        implicit val ec = system.dispatcher
        implicit val _system = system
        val log = LogFactory.getLog(this.getClass)

        log.info(s"Subscribing to $streamName")

        val worker = new Worker.Builder()
          .recordProcessorFactory(new RecordProcessorFactory(ref)(decoder, _system))
          .config(kinesisClientConfig)
          .build()

        Future(blocking {
          log.info(s"Starting worker for $streamName")
          worker.run()
        })
          .map(Status.Success(_))
          .recover { case ex => Status.Failure(ex) }
          .pipeTo(ref)

        worker
      }.watchTermination() { (worker, doneF) =>
        implicit val _ec = system.dispatcher
        val log = LogFactory.getLog(this.getClass)
        doneF.andThen { case _ =>
          log.info("Shutting down worker after stream done")
          Try(worker.shutdown())
        }
        NotUsed
      }
    }
}
