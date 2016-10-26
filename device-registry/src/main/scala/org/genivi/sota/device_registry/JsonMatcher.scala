package org.genivi.sota.device_registry

import io.circe.{Json, JsonObject}

object JsonMatcher {

  /* disregards discarded attribute sets
   *
   * @return cleansed json
   */
  def disregard(json: Json, discarded: Json): Json =
    json.fold(
      Json.Null,
      b => json,
      n => json,
      s => json,
      a => json,
      o => if (discarded.isObject) disregardObjects(o, discarded.asObject.get)
           else json
    )

  def disregardObjects(json: JsonObject, discarded: JsonObject): Json = {
    val discardedKeys =
      discarded
        .toList
        .filter { case (k, v) => v.isNull }
        .map(_._1)

    Json.fromFields(json
      .toList
      .filter { case (k, v) => !(discardedKeys contains k) }
      .map { case (k, v) => discarded(k) match {
        case None    => k -> v
        case Some(j) => k -> disregard(v, j)
      }})
  }

  /* Compares two JSON values and returns a tuple consisting of the common part
   *
   * @return a tuple consisting of the common component and the diverging part
   */
  def compare(lhs: Json, rhs: Json): (Json, Json) =
    lhs.fold(
      (Json.Null, Json.Null),
      b => if (lhs equals rhs) (lhs, Json.Null)
           else (Json.Null, Json.Null),
      n => if (lhs equals rhs)  (lhs, Json.Null)
           else (Json.Null, Json.Null),
      s => if (lhs equals rhs)  (lhs, Json.Null)
           else (Json.Null, Json.Null),
      a => if (rhs.isArray)  compareArrays(a, rhs.asArray.get)
           else (Json.Null, Json.Null),
      o => if (rhs.isObject) compareObjects(o, rhs.asObject.get)
           else (Json.Null, Json.Null)
    )


  def compareArrays(lhs: Seq[Json], rhs: Seq[Json]): (Json, Json) = {
    val common =
      Json.fromValues(
        lhs.zip(rhs)
          .map { case (l, r) => compare(l, r)._1 }
          .filter(!_.isNull))

    (common, Json.Null)
  }

  def compareObjects(lhs: JsonObject, rhs: JsonObject): (Json, Json) = {
    val common =
      (lhs.fieldSet & rhs.fieldSet)
        .map(k => k -> compare(lhs(k).get, rhs(k).get)._1)
        .filter { case (k, v) => !v.isNull }

    val uncommon =
      (lhs.fieldSet & rhs.fieldSet) // common objects and arrays result in a child
        .map(k => k -> compare(lhs(k).get, rhs(k).get)._2)
        .filter { case (k, v) => !v.isNull } ++
      ((lhs.fieldSet | rhs.fieldSet) &~ (lhs.fieldSet & rhs.fieldSet)) // different attrs result in null
        .map(k => k -> Json.Null) ++
      (lhs.fieldSet & rhs.fieldSet) // different literals result in null
        .filter(k => !lhs(k).get.isObject && !lhs(k).get.isArray &&
                     !rhs(k).get.isObject && !rhs(k).get.isArray &&
                     lhs(k) != rhs(k))
        .map(k => k -> Json.Null)

    (if (common.isEmpty)   Json.Null else Json.fromFields(common),
     if (uncommon.isEmpty) Json.Null else Json.fromFields(uncommon))
  }

}
