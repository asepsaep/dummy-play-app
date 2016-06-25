package controllers

import javax.inject.{ Inject, Singleton }

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import forms.UsernameConfirmationForm
import models.daos.AccountDAO
import models.services.AccountService
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ Action, Controller }
import play.api.Environment
import utils.auth.{ DefaultEnv, WithActiveAccount, WithProvider }

import scala.concurrent.Future

/**
 * The social auth controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param accountService The account service implementation.
 * @param accountDao The account dao implementation.
 * @param authInfoRepository The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 * @param webJarAssets The webjar assets implementation.
 */

@Singleton
class SocialAuthController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  accountService: AccountService,
  accountDao: AccountDAO,
  authInfoRepository: AuthInfoRepository,
  socialProviderRegistry: SocialProviderRegistry,
  implicit val webJarAssets: WebJarAssets,
  implicit val environment: Environment
)
  extends Controller with I18nSupport with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit request ⇒
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) ⇒
        p.authenticate().flatMap {
          case Left(result) ⇒ Future.successful(result)
          case Right(authInfo) ⇒ for {
            profile ← p.retrieveProfile(authInfo)
            account ← accountService.save(profile)
            authInfo ← authInfoRepository.save(profile.loginInfo, authInfo)
            authenticator ← silhouette.env.authenticatorService.create(profile.loginInfo)
            value ← silhouette.env.authenticatorService.init(authenticator)
            result ← if (account.active) silhouette.env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index())) else silhouette.env.authenticatorService.embed(value, Redirect(routes.SocialAuthController.view()))
          } yield {
            result
          }
        }
      case _ ⇒ Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException ⇒
        logger.error("Unexpected provider error", e)
        Redirect(routes.SignInController.view()).flashing("error" → Messages("could.not.authenticate"))
    }
  }

  def view = silhouette.SecuredAction(WithProvider(CredentialsProvider.ID) && !WithActiveAccount()).async { implicit request ⇒
    Future.successful(Ok(views.html.usernameConfirmation(UsernameConfirmationForm.form)))
  }

  def submit = silhouette.SecuredAction(WithProvider(CredentialsProvider.ID) && !WithActiveAccount()).async { implicit request ⇒
    UsernameConfirmationForm.form.bindFromRequest.fold(
      form ⇒ Future.successful(BadRequest(views.html.usernameConfirmation(form))),
      data ⇒ {
        accountDao.find(data.username).flatMap {
          case Some(account) ⇒ Future.successful(Redirect(routes.SocialAuthController.view()).flashing("error" → Messages("user.exists")))
          case None ⇒ {
            val account = request.identity.copy(username = data.username, active = true)
            for {
              updatedAccount ← accountDao.updateUsername(request.identity.username, account)
              authenticator ← silhouette.env.authenticatorService.create(updatedAccount.loginInfo)
              value ← silhouette.env.authenticatorService.init(authenticator)
              result ← silhouette.env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index()))
            } yield {
              result
            }
          }
        }
      }
    )
  }

}
