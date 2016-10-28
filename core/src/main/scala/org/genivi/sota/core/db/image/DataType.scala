/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.db.image

import java.time.Instant
import java.util.UUID

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.string.Uri
import org.genivi.sota.core.data.UpdateStatus._
import org.genivi.sota.data.{Namespace, Uuid}

object DataType {
  case class ImageId(get: UUID) extends AnyVal

  object ImageId {
    protected[db] def generate(): ImageId = ImageId(UUID.randomUUID())
  }

  case class Image(namespace: Namespace,
                   id: ImageId,
                   commit: Commit,
                   ref: RefName,
                   description: String,
                   pullUri: PullUri,
                   createdAt: Instant,
                   updatedAt: Instant)

  case class ValidCommit()

  type Commit = Refined[String, ValidCommit]

  implicit val validCommit: Validate.Plain[String, ValidCommit] =
    Validate.fromPredicate(
      hash => hash.length == 64 && hash.forall(h => ('0' to '9').contains(h) || ('a' to 'f').contains(h)),
      hash => s"$hash is not a sha-256 commit hash",
      ValidCommit()
    )

  case class ImageUpdateId(get: UUID) extends AnyVal

  object ImageUpdateId {
    protected[db] def generate(): ImageUpdateId = ImageUpdateId(UUID.randomUUID())
  }

  type PullUri = Refined[String, Uri]

  case class RefName(get: String) extends AnyVal

  case class ImageUpdate(namespace: Namespace,
                         id: ImageUpdateId,
                         imageId: ImageId,
                         device: Uuid,
                         status: UpdateStatus,
                         createdAt: Instant,
                         updatedAt: Instant)
}
