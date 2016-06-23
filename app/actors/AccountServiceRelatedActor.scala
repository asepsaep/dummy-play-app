package actors

import javax.inject.Inject

import akka.actor.Actor.Receive
import akka.actor._
import com.mohiva.play.silhouette.api.{ Logger, LoginInfo }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import events._
import models.{ Account, TokenInfo }
import models.daos.TokenInfoDAO
import models.services.AccountService
import utils.EmailSender

object AccountServiceRelatedActor {
  def props = Props[AccountServiceRelatedActor]
}

class AccountServiceRelatedActor @Inject() (
  tokenInfoDAO:       TokenInfoDAO,
  emailSender:        EmailSender,
  passwordHasher:     PasswordHasher,
  authInfoRepository: AuthInfoRepository,
  accountService:     AccountService
) extends Actor {

  import AccountServiceRelatedActor._

  override def receive: Receive = {

    case event @ CredentialsSignUpEvent(account) => {
      val tokenInfo: TokenInfo = TokenInfo(email = account.email.get, accountUsername = account.username)
      tokenInfoDAO.save(tokenInfo)
      emailSender.emailConfirmation(tokenInfo, account)
    }

    case event @ EmailConfirmedEvent(tokenInfo) => {
      accountService.activate(tokenInfo.accountUsername)
      tokenInfoDAO.delete(tokenInfo.token)
    }

    case event @ RequestResetPasswordEvent(account) => {
      val tokenInfo: TokenInfo = TokenInfo(email = account.email.get, accountUsername = account.username)
      tokenInfoDAO.save(tokenInfo)
      emailSender.resetPassword(tokenInfo, account)
    }

    case event @ ResetPasswordEvent(tokenInfo, newPassword) => {
      val authInfo = passwordHasher.hash(newPassword)
      val loginInfo = LoginInfo(CredentialsProvider.ID, tokenInfo.accountUsername)
      authInfoRepository.update(loginInfo, authInfo)
      tokenInfoDAO.delete(tokenInfo.token)
    }

  }

}
