package controllers

import javax.inject.{ Inject, Singleton }

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import com.nappin.play.recaptcha.{ RecaptchaVerifier, WidgetHelper }
import forms.LoginForm
import models.services.AccountService
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.Logger
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ AnyContent, Controller, Request }
import play.api.Environment
import play.api.data.Form
import utils.AppMode
import utils.auth.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class LoginController @Inject()(
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  accountService: AccountService,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  socialProviderRegistry: SocialProviderRegistry,
  configuration: Configuration,
  clock: Clock,
  eventBus: EventBus,
  val verifier: RecaptchaVerifier,
  implicit val appMode: AppMode,
  implicit val webJarAssets: WebJarAssets,
  implicit val environment: Environment
)(implicit widgetHelper: WidgetHelper)
  extends Controller with I18nSupport {

  def view = silhouette.UnsecuredAction.async { implicit request ⇒
    Future.successful(Ok(views.html.login(LoginForm.form, socialProviderRegistry)))
  }

  def submit = silhouette.UnsecuredAction.async { implicit request ⇒
    if (appMode.isProd) {
      verifier.bindFromRequestAndVerify(LoginForm.form).flatMap { form ⇒
        formFoldHelper(form)
      }
    } else {
      formFoldHelper(LoginForm.form.bindFromRequest)
    }
  }

  def formFoldHelper(form: Form[LoginForm.Data])(implicit request: Request[AnyContent]) = {
    form.fold(
      form ⇒ Future.successful(BadRequest(views.html.login(form, socialProviderRegistry))),
      data ⇒ {
        val credentials = Credentials(data.username, data.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo ⇒
          val result = Redirect(routes.ApplicationController.index())
          accountService.retrieve(loginInfo).flatMap {
            case Some(account) ⇒ {
              val c = configuration.underlying
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator if data.rememberMe ⇒
                  authenticator.copy(
                    expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                  )
                case authenticator ⇒ authenticator
              }.flatMap { authenticator ⇒
                eventBus.publish(LoginEvent(account, request))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v ⇒
                  silhouette.env.authenticatorService.embed(v, result)
                }
              }
            }
            case None ⇒ {
              Future.failed(new IdentityNotFoundException("Couldn't find user"))
            }
          }
        }.recover {
          case e: ProviderException ⇒
            Logger.error(e.getMessage + " " + e.getCause)
            Redirect(routes.LoginController.view()).flashing("error" → Messages("invalid.credentials"))
        }
      }
    )
  }

}
