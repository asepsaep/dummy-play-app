package models

import com.mohiva.play.silhouette.impl.providers.OAuth1Info

import scala.language.implicitConversions

case class DBOAuth1Info(
  token:           String,
  secret:          String,
  accountUsername: String
)

object DBOAuth1Info {

  def oAuth1Info2db(accountUsername: String, oAuth1Info: OAuth1Info) = {
    DBOAuth1Info(accountUsername = accountUsername, token = oAuth1Info.token, secret = oAuth1Info.secret)
  }

  implicit def db2OAuth1Info(dbOAuth1Info: DBOAuth1Info): OAuth1Info = {
    OAuth1Info(token = dbOAuth1Info.token, secret = dbOAuth1Info.secret)
  }

  implicit def dbTableElement2OAuth1Info(dbOAuth1Info: OAuth1InfoTable#TableElementType): OAuth1Info = {
    OAuth1Info(token = dbOAuth1Info.token, secret = dbOAuth1Info.secret)
  }

}