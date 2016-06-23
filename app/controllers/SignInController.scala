package controllers

import javax.inject.{ Inject, Singleton }

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import forms.SignInForm
import models.services.AccountService
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.Logger
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import utils.auth.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * The `Sign In` controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param accountService The account service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param credentialsProvider The credentials provider.
 * @param socialProviderRegistry The social provider registry.
 * @param configuration The Play configuration.
 * @param clock The clock instance.
 * @param webJarAssets The webjar assets implementation.
 */

@Singleton
class SignInController @Inject() (
  val messagesApi:           MessagesApi,
  silhouette:                Silhouette[DefaultEnv],
  accountService:            AccountService,
  authInfoRepository:        AuthInfoRepository,
  credentialsProvider:       CredentialsProvider,
  socialProviderRegistry:    SocialProviderRegistry,
  configuration:             Configuration,
  clock:                     Clock,
  implicit val webJarAssets: WebJarAssets
)
  extends Controller with I18nSupport {

  /**
   * Views the `Sign In` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signIn(SignInForm.form, socialProviderRegistry)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form, socialProviderRegistry))),
      data => {
        val credentials = Credentials(data.username, data.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          val result = Redirect(routes.ApplicationController.index())
          accountService.retrieve(loginInfo).flatMap {
            case Some(account) => {
              Logger.info(account.toString)
              val c = configuration.underlying
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator if data.rememberMe =>
                  authenticator.copy(
                    expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                  )
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(account, request))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                  silhouette.env.authenticatorService.embed(v, result)
                }
              }
            }
            case None => {
              Future.failed(new IdentityNotFoundException("Couldn't find user"))
            }
          }
        }.recover {
          case e: ProviderException =>
            Logger.error(e.getMessage + " " + e.getCause)
            Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.credentials"))
        }
      }
    )
  }
}
