package org.genivi.sota.messaging.kinesis

import java.nio.ByteBuffer

import akka.NotUsed
import akka.actor.{ActorSystem, Status}
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
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration}
import com.typesafe.config.{Config, ConfigException}
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.Messages.MessageLike
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.Try
import akka.pattern.pipe

object KinesisClient {

  private[this] def getClientConfigWithUserAgent(appName: String, version: String): ClientConfiguration = {
    val config = new ClientConfiguration
    config.setUserAgent(s"${ClientConfiguration.DEFAULT_USER_AGENT} $appName/$version")
    config
  }

  private[this] def configureCredentialsProvider(config: Config): ConfigException Xor AWSCredentialsProvider =
    for {
      awsConfig <- config.configAt("aws")
      awsAccessKey <- awsConfig.readString("accessKeyId")
      awsSecretKey <- awsConfig.readString("secretAccessKey")
    } yield new StaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey))

  private[this] def streamSuffix(config: Config): ConfigException Xor (String => String) =
    for {
      cfg <- config.configAt("messaging.kinesis")
      suffix <- cfg.readString("streamNameSuffix")
    } yield (streamName: String) => streamName + "-" + suffix

  private[this] def getAmazonClient(system: ActorSystem, config: Config): Throwable Xor AmazonKinesisClient =
    for {
      cfg <- config.configAt("messaging.kinesis")
      appName <- cfg.readString("appName")
      regionName <- cfg.readString("regionName")
      region = Regions.fromName(regionName)
      version <- cfg.readString("appVersion")
      clientConfig = getClientConfigWithUserAgent(appName, version)
      credentials <- configureCredentialsProvider(config)
      client = new AmazonKinesisClient(credentials, clientConfig)
      _ <- Xor.fromTry(Try(client.configureRegion(region)))
    } yield {
      system.registerOnTermination(client.shutdown())
      client
    }

  def publisher(system: ActorSystem, config: Config): Throwable Xor MessageBusPublisher = {
    for {
      streamNameFn <- streamSuffix(config)
      client <- getAmazonClient(system, config)
    } yield {
      new MessageBusPublisher {
        override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] = {
          Future {
            blocking {
              client.putRecord(
                streamNameFn(messageLike.streamName),
                ByteBuffer.wrap(msg.asJson(messageLike.encoder).noSpaces.getBytes),
                messageLike.partitionKey(msg))

              ()
            }
          }
        }
      }
    }
  }

  def source[T](system: ActorSystem, config: Config)(implicit ml: MessageLike[T])
                             : Throwable Xor Source[T, NotUsed] =
    for {
      cfg <- config.configAt("messaging.kinesis")
      streamName <- streamSuffix(config).map(f => f(ml.streamName))
      appName <- cfg.readString("appName").map(_ + "-" + streamName)
      regionName <- cfg.readString("regionName")
      version <- cfg.readString("appVersion")
      clientConfig = getClientConfigWithUserAgent(appName, version)
      credentials <- configureCredentialsProvider(config)
    } yield {
      val kinesisClientConfig = new KinesisClientLibConfiguration(
        appName, streamName, credentials, s"$streamName-worker")
        .withRegionName(regionName)
        .withCommonClientConfig(clientConfig)
        .withInitialPositionInStream(InitialPositionInStream.LATEST)

      implicit val ec = system.dispatcher
      implicit val _system = system
      val log = system.log

      Source.actorRef[T](MessageBus.DEFAULT_CLIENT_BUFFER_SIZE,
        OverflowStrategy.dropHead).mapMaterializedValue { ref =>

        val worker = new Worker.Builder()
          .recordProcessorFactory(new RecordProcessorFactory(ref)(ml.decoder, _system))
          .config(kinesisClientConfig)
          .build()

        log.info(s"Starting worker for $streamName")

        Future { blocking { worker.run() } }
          .map(Status.Success(_))
          .recover { case ex => Status.Failure(ex) }
          .pipeTo(ref)

        worker
      }.watchTermination() { (worker, doneF) =>
        doneF.andThen { case _ =>
          log.info(s"Shutting down worker after stream done ($streamName)")
          Try(worker.shutdown())
        }
        NotUsed
      }
    }
}
