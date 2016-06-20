package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers._
import forms.SignUpForm
import models.Account
import models.services.AccountService
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import play.api.Logger
import utils.auth.DefaultEnv

import scala.concurrent.Future

/**
 * The `Sign Up` controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param accountService The account service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param avatarService The avatar service implementation.
 * @param passwordHasher The password hasher implementation.
 * @param webJarAssets The webjar assets implementation.
 */
class SignUpController @Inject() (
  val messagesApi:           MessagesApi,
  silhouette:                Silhouette[DefaultEnv],
  accountService:            AccountService,
  authInfoRepository:        AuthInfoRepository,
  avatarService:             AvatarService,
  passwordHasher:            PasswordHasher,
  implicit val webJarAssets: WebJarAssets
)
  extends Controller with I18nSupport {

  /**
   * Views the `Sign Up` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signUp(SignUpForm.form)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signUp(form))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.username)
        accountService.retrieve(loginInfo).flatMap {
          case Some(account) =>
            Future.successful(Redirect(routes.SignUpController.view()).flashing("error" -> Messages("user.exists")))
          case None =>
            val authInfo = passwordHasher.hash(data.password)
            val account = Account(
              providerId = loginInfo.providerID,
              providerKey = loginInfo.providerKey,
              username = data.username,
              name = Some(data.name),
              email = Some(data.email)
            )
            Logger.info(account.toString)
            for {
              avatar <- avatarService.retrieveURL(data.email)
              account <- accountService.save(account.copy(avatarUrl = avatar))
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              value <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index()))
            } yield {
              silhouette.env.eventBus.publish(SignUpEvent(account, request))
              silhouette.env.eventBus.publish(LoginEvent(account, request))
              result
            }
        }
      }
    )
  }
}
