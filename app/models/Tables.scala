package models

import java.time.OffsetDateTime

import modules.MyPostgresDriver.api._

class AccountTable(tag: Tag) extends Table[Account](tag, "account") {

  def username = column[String]("username", O.PrimaryKey)
  def name = column[String]("name")
  def email = column[String]("email")
  def userType = column[String]("user_type")
  def userParent = column[String]("user_parent")
  def active = column[Boolean]("active")
  def avatarUrl = column[String]("avatar_url")
  def createdAt = column[OffsetDateTime]("created_at")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")

  override def * = (username, name.?, email.?, userType.?, userParent.?, active, avatarUrl.?, createdAt, providerId, providerKey) <> (Account.tupled, Account.unapply _)

}

class PasswordInfoTable(tag: Tag) extends Table[DBPasswordInfo](tag, "password_info") {

  def hasher = column[String]("hasher")
  def password = column[String]("password")
  def salt = column[String]("salt")
  def accountUsername = column[String]("account_username")

  override def * = (hasher, password, salt.?, accountUsername) <> ((DBPasswordInfo.apply _).tupled, DBPasswordInfo.unapply _)

}

class OAuth1InfoTable(tag: Tag) extends Table[DBOAuth1Info](tag, "oauth1_info") {

  def token = column[String]("token")
  def secret = column[String]("secret")
  def accountUsername = column[String]("account_username")

  override def * = (token, secret, accountUsername) <> ((DBOAuth1Info.apply _).tupled, DBOAuth1Info.unapply _)

}

class OAuth2InfoTable(tag: Tag) extends Table[DBOAuth2Info](tag, "oauth2_info") {

  def accessToken = column[String]("access_token")
  def tokenType = column[String]("token_type")
  def expiresIn = column[Int]("expires_in")
  def refreshToken = column[String]("refresh_token")
  def params = column[Map[String, String]]("params")
  def accountUsername = column[String]("account_username")

  override def * = (accessToken, tokenType.?, expiresIn.?, refreshToken.?, params.?, accountUsername) <> ((DBOAuth2Info.apply _).tupled, DBOAuth2Info.unapply _)

}