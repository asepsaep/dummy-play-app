package controllers

import javax.inject.{ Inject, Singleton }

import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import play.api.Environment
import utils.auth.DefaultEnv

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

@Singleton
class ApplicationController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  implicit val webJarAssets: WebJarAssets,
  implicit val environment: Environment
)
  extends Controller with I18nSupport {

  def index = silhouette.SecuredAction.async { implicit request ⇒
    Future.successful(Ok(views.html.home(request.identity)))
  }

  def logout = silhouette.SecuredAction.async { implicit request ⇒
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

}
