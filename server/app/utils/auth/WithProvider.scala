package utils.auth

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.Account
import play.api.mvc.Request

import scala.concurrent.Future

case class WithProvider(provider: String) extends Authorization[Account, CookieAuthenticator] {

  def isAuthorized[B](account: Account, authenticator: CookieAuthenticator)(implicit request: Request[B]) = {
    Future.successful(account.providerId == provider)
  }

}