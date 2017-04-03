package org.genivi.sota.data

import java.util.Base64

import io.circe.{Decoder, Encoder, Json}

final case class OstreeSummary(bytes: Array[Byte])

object OstreeSummary {
  implicit val ostreeSummaryEncoder: Encoder[OstreeSummary] = Encoder.instance { summary =>
    Json.obj("bytes" -> Json.fromString(Base64.getEncoder.encodeToString(summary.bytes)))
  }

  implicit val ostreeSummaryDecoder: Decoder[OstreeSummary] = Decoder.instance { json =>
    import cats.syntax.either._
    json.downField("bytes").as[String].map { bytes =>
      OstreeSummary(Base64.getDecoder.decode(bytes))
    }
  }

}
