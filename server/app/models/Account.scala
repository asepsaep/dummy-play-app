package models

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

case class Account(
  username: String,
  name: Option[String],
  email: Option[String],
  userType: Option[String] = None,
  userParent: Option[String] = None,
  active: Boolean = true,
  avatarUrl: Option[String] = None,
  createdAt: OffsetDateTime = OffsetDateTime.now(),
  providerId: String,
  providerKey: String
) extends Identity {

  def loginInfo = LoginInfo(providerId, providerKey)

  override def toString = {
    s"""
       |Username = $username
       |Name = $name
       |Email = $email
       |User Type = $userType
       |User Parent = $userParent
       |Active = $active
       |Avatar URL = $avatarUrl
       |Created at = $createdAt
       |Provider ID = $providerId
       |Provider Key = $providerKey
     """.stripMargin
  }

}