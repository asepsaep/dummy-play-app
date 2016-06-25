package models

import com.mohiva.play.silhouette.api.util.PasswordInfo
import scala.language.implicitConversions

case class DBPasswordInfo(
  hasher: String,
  password: String,
  salt: Option[String],
  accountUsername: String
)

object DBPasswordInfo {

  def passwordInfo2db(accountUsername: String, passwordInfo: PasswordInfo) = {
    DBPasswordInfo(
      hasher = passwordInfo.hasher,
      password = passwordInfo.password,
      salt = passwordInfo.salt,
      accountUsername = accountUsername
    )
  }

  implicit def db2PasswordInfo(passwordInfo: DBPasswordInfo): PasswordInfo = {
    new PasswordInfo(passwordInfo.hasher, passwordInfo.password, passwordInfo.salt)
  }

  implicit def dbTableElement2PasswordInfo(passwordInfo: PasswordInfoTable#TableElementType): PasswordInfo = {
    new PasswordInfo(passwordInfo.hasher, passwordInfo.password, passwordInfo.salt)
  }

}

