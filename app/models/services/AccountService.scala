package models.services

import java.time.OffsetDateTime
import javax.inject.Inject

import play.api.Logger

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.Account
import models.daos.AccountDAO

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

trait AccountService extends IdentityService[Account] {
  def save(user: Account): Future[Account]
  def save(profile: CommonSocialProfile): Future[Account]
}

class AccountServiceImpl @Inject() (accountDAO: AccountDAO) extends AccountService {

  override def retrieve(loginInfo: LoginInfo): Future[Option[Account]] = {
    accountDAO.findByLoginInfo(loginInfo)
  }

  override def save(account: Account): Future[Account] = {
    accountDAO.save(account)
  }

  // TODO -- Fix this shit
  override def save(profile: CommonSocialProfile): Future[Account] = {
    accountDAO.findByLoginInfo(profile.loginInfo).flatMap {
      case Some(account) =>
        accountDAO.save(account.copy(
          name = profile.fullName,
          email = profile.email,
          avatarUrl = profile.avatarURL
        ))
      case None =>
        accountDAO.save(Account(
          username = util.Random.alphanumeric.take(30).mkString,
          name = profile.fullName,
          email = profile.email,
          avatarUrl = profile.avatarURL,
          active = false,
          createdAt = OffsetDateTime.now(),
          providerId = profile.loginInfo.providerID,
          providerKey = profile.loginInfo.providerKey
        ))
    }
  }

}
