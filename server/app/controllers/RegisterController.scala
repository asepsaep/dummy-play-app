package controllers

import javax.inject.{ Inject, Named, Singleton }

import akka.actor._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers._
import com.nappin.play.recaptcha.{ RecaptchaVerifier, WidgetHelper }
import events.{ CredentialsSignUpEvent, EmailConfirmedEvent }
import forms.RegisterForm
import models.Account
import models.daos.TokenInfoDAO
import models.services.AccountService
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ Action, AnyContent, Controller, Request }
import play.api.Environment
import play.api.data.Form
import utils.AppMode
import utils.auth.DefaultEnv

import scala.concurrent.Future

@Singleton
class RegisterController @Inject()(
                                    val messagesApi: MessagesApi,
                                    silhouette: Silhouette[DefaultEnv],
                                    accountService: AccountService,
                                    authInfoRepository: AuthInfoRepository,
                                    avatarService: AvatarService,
                                    tokenInfoDAO: TokenInfoDAO,
                                    passwordHasher: PasswordHasher,
                                    @Named("account-service-related-actor") registerActor: ActorRef,
                                    eventBus: EventBus,
                                    val verifier: RecaptchaVerifier,
                                    implicit val appMode: AppMode,
                                    implicit val webJarAssets: WebJarAssets,
                                    implicit val environment: Environment
)(implicit widgetHelper: WidgetHelper)
  extends Controller with I18nSupport {

  eventBus.subscribe(registerActor, classOf[CredentialsSignUpEvent])
  eventBus.subscribe(registerActor, classOf[EmailConfirmedEvent])

  def view = silhouette.UnsecuredAction.async { implicit request ⇒
    Future.successful(Ok(views.html.register(RegisterForm.form)))
  }

  def submit = silhouette.UnsecuredAction.async { implicit request ⇒
    if (appMode.isProd) {
      verifier.bindFromRequestAndVerify(RegisterForm.form).flatMap { form ⇒
        formFoldHelper(form)
      }
    } else {
      formFoldHelper(RegisterForm.form.bindFromRequest)
    }
  }

  def formFoldHelper(form: Form[RegisterForm.Data])(implicit request: Request[AnyContent]) = {
    form.fold(
      form ⇒ Future.successful(BadRequest(views.html.register(form))),
      data ⇒ {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.username)
        accountService.retrieveAlongWithEmail(loginInfo, data.email).flatMap {
          case Some(account) ⇒
            Future.successful(Redirect(routes.RegisterController.view()).flashing("error" → Messages("user.exists")))
          case None ⇒
            val authInfo = passwordHasher.hash(data.password)
            val account = Account(
              providerId = loginInfo.providerID,
              providerKey = loginInfo.providerKey,
              username = data.username,
              active = false,
              name = Some(data.name),
              email = Some(data.email)
            )
            for {
              avatar ← avatarService.retrieveURL(data.email)
              account ← accountService.save(account.copy(avatarUrl = avatar))
              authInfo ← authInfoRepository.add(loginInfo, authInfo)
              authenticator ← silhouette.env.authenticatorService.create(loginInfo)
              value ← silhouette.env.authenticatorService.init(authenticator)
              result ← silhouette.env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index()))
            } yield {
              eventBus.publish(SignUpEvent(account, request))
              eventBus.publish(CredentialsSignUpEvent(account))
              eventBus.publish(LoginEvent(account, request))
              result
            }
        }
      }
    )
  }

  def emailConfirmation(token: String, email: String) = Action.async { implicit request ⇒
    tokenInfoDAO.find(token).flatMap {
      case None ⇒ Future.successful(Redirect(routes.ApplicationController.index()))
      case Some(tokenInfo) ⇒ {
        if (tokenInfo.email.toLowerCase == email.toLowerCase) {
          eventBus.publish(EmailConfirmedEvent(tokenInfo))
          Future.successful(Redirect(routes.ApplicationController.index()).flashing("email.confirm" → Messages("email.confirm.success")))
        } else {
          Future.successful(Redirect(routes.ApplicationController.index()))
        }
      }
    }
  }

}
