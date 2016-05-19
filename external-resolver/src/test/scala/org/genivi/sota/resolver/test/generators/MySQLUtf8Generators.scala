package org.genivi.sota.resolver.test.generators

import org.scalacheck.Gen

/**
  * In MySQL, the character set named utf8 allows only three bytes per character and contains only BMP characters.
  * For arbitrary UTF8, the MySQL character set is utf8mb4 (which takes 4 bytes per character).
  * BMP in UTF8, description at https://en.wikipedia.org/wiki/Plane_%28Unicode%29#Basic_Multilingual_Plane
  * <br>
  * Apparently Scalacheck `arbString` usually generates strings with BMP except when there's an off-by-one error.
  * On the other hand, `Gen.identifier` contain ASCII-only chars.
  */
trait MySQLUtf8Generators {

  val UnicodeLeadingSurrogate = '\uD800' to '\uDBFF'
  val UnicodeTrailingSurrogate = '\uDC00' to '\uDFFF'
  val UnicodeBasicMultilingualPlane =
    ('\u0000' to '\uFFFF').diff(UnicodeLeadingSurrogate).diff(UnicodeTrailingSurrogate)

  val unicodeCharacterBasicMultilingualPlane: Gen[String] = Gen.oneOf(UnicodeBasicMultilingualPlane).map(_.toString)

  val unicodeCharacterSupplementaryPlane: Gen[String] = for {
    c1 <- Gen.oneOf(UnicodeLeadingSurrogate)
    c2 <- Gen.oneOf(UnicodeTrailingSurrogate)
  } yield {
    c1.toString + c2.toString
  }

  val unrestrictedUnicodeCharacter = Gen.frequency(
    9 -> unicodeCharacterBasicMultilingualPlane,
    1 -> unicodeCharacterSupplementaryPlane)

  val unicodeStringForMySQLUtf8 = Gen.listOf(unicodeCharacterBasicMultilingualPlane).map(_.mkString)

  def getUnicodeStringForMySQLUtf8: String = unicodeStringForMySQLUtf8.sample.getOrElse(getUnicodeStringForMySQLUtf8)

}
