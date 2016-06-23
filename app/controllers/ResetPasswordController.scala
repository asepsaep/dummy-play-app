package controllers

import java.time.OffsetDateTime
import javax.inject.{ Inject, Named, Singleton }

import akka.actor._
import com.mohiva.play.silhouette.api.{ EventBus, Silhouette }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.nappin.play.recaptcha.{ RecaptchaVerifier, WidgetHelper }
import events.{ RequestResetPasswordEvent, ResetPasswordEvent }
import forms.{ RequestResetPassword, ResetPassword }
import models.daos.TokenInfoDAO
import models.services.AccountService
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import utils.auth.DefaultEnv

import scala.concurrent.Future

@Singleton
class ResetPasswordController @Inject() (
  val messagesApi:                                            MessagesApi,
  silhouette:                                                 Silhouette[DefaultEnv],
  authInfoRepository:                                         AuthInfoRepository,
  accountService:                                             AccountService,
  tokenInfoDAO:                                               TokenInfoDAO,
  passwordHasher:                                             PasswordHasher,
  eventBus:                                                   EventBus,
  @Named("account-service-related-actor") resetPasswordActor: ActorRef,
  val verifier:                                               RecaptchaVerifier,
  implicit val webJarAssets:                                  WebJarAssets
)(implicit widgetHelper: WidgetHelper) extends Controller with I18nSupport {

  eventBus.subscribe(resetPasswordActor, classOf[RequestResetPasswordEvent])
  eventBus.subscribe(resetPasswordActor, classOf[ResetPasswordEvent])

  def viewRequestResetPassword = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.requestResetPassword(RequestResetPassword.form)))
  }

  def submitRequestResetPassword = silhouette.UnsecuredAction.async { implicit request =>
    verifier.bindFromRequestAndVerify(RequestResetPassword.form).flatMap { form =>
      form.fold(
        form => Future.successful(BadRequest(views.html.requestResetPassword(form))),
        data => {
          accountService.findCredentialsAccount(data.email).flatMap {
            case None => Future.successful(Redirect(routes.ResetPasswordController.viewRequestResetPassword()).flashing("error" -> Messages("request.reset.password.noemail")))
            case Some(account) => {
              eventBus.publish(RequestResetPasswordEvent(account))
              Future.successful(Redirect(routes.SignInController.view()).flashing("request.reset.password.info" -> Messages("request.reset.password.info")))
            }
          }
        }
      )
    }
  }

  def viewResetPassword(token: String, email: String) = silhouette.UnsecuredAction.async { implicit request =>
    tokenInfoDAO.find(token).flatMap {
      case None => Future.successful(Redirect(routes.ApplicationController.index()))
      case Some(tokenInfo) => {
        if (tokenInfo.email.toLowerCase == email.toLowerCase) Future.successful(Ok(views.html.resetPassword(ResetPassword.form, token, email)))
        else Future.successful(Redirect(routes.ApplicationController.index()))
      }
    }
  }

  def submitResetPassword(token: String, email: String) = silhouette.UnsecuredAction.async { implicit request =>
    ResetPassword.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.resetPassword(form, token, email))),
      data => {
        tokenInfoDAO.find(token).flatMap {
          case None => Future.successful(Redirect(routes.ApplicationController.index()))
          case Some(tokenInfo) => {
            if (tokenInfo.email.toLowerCase != email.toLowerCase) Future.successful(Redirect(routes.ApplicationController.index()))
            else if (tokenInfo.expiresAt.compareTo(OffsetDateTime.now()) < 0) Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("request.reset.password.expire")))
            else if (data.password != data.confirmPassword) Future.successful(Redirect(routes.ResetPasswordController.viewResetPassword(token, email)).flashing("error" -> Messages("request.reset.password.nomatch")))
            else {
              eventBus.publish(ResetPasswordEvent(tokenInfo, data.password))
              Future.successful(Redirect(routes.SignInController.view()).flashing("reset.password.status" -> Messages("request.reset.password.status")))
            }
          }
        }
      }
    )
  }

}
