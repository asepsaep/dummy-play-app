package models

import java.time.OffsetDateTime

case class TokenInfo(
  token:           String         = util.Random.alphanumeric.take(64).mkString,
  email:           String,
  expiresAt:       OffsetDateTime = OffsetDateTime.now().plusHours(1),
  accountUsername: String
) {

  def isValid() = expiresAt.compareTo(OffsetDateTime.now()) > 0

}
