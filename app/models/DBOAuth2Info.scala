package models

import com.mohiva.play.silhouette.impl.providers.OAuth2Info

import scala.language.implicitConversions

case class DBOAuth2Info(
  accessToken:     String,
  tokenType:       Option[String]              = None,
  expiresIn:       Option[Int]                 = None,
  refreshToken:    Option[String]              = None,
  params:          Option[Map[String, String]] = None,
  accountUsername: String
)

object DBOAuth2Info {

  def oAuth2Info2db(accountUsername: String, oAuth2Info: OAuth2Info) = {
    DBOAuth2Info(
      accessToken = oAuth2Info.accessToken,
      tokenType = oAuth2Info.tokenType,
      expiresIn = oAuth2Info.expiresIn,
      refreshToken = oAuth2Info.refreshToken,
      params = oAuth2Info.params,
      accountUsername = accountUsername
    )
  }

  implicit def db2OAuth2Info(dbOAuth2Info: DBOAuth2Info): OAuth2Info = {
    OAuth2Info(
      accessToken = dbOAuth2Info.accessToken,
      tokenType = dbOAuth2Info.tokenType,
      expiresIn = dbOAuth2Info.expiresIn,
      refreshToken = dbOAuth2Info.refreshToken,
      params = dbOAuth2Info.params
    )
  }

  implicit def dbTableElement2OAuth1Info(dbOAuth2Info: OAuth2InfoTable#TableElementType): OAuth2Info = {
    OAuth2Info(
      accessToken = dbOAuth2Info.accessToken,
      tokenType = dbOAuth2Info.tokenType,
      expiresIn = dbOAuth2Info.expiresIn,
      refreshToken = dbOAuth2Info.refreshToken,
      params = dbOAuth2Info.params
    )
  }

}