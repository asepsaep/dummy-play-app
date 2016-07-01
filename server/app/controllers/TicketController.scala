package controllers

import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.nappin.play.recaptcha.{RecaptchaVerifier, WidgetHelper}
import forms.TicketForm
import models.Ticket
import models.daos.{MilestoneDAO, TicketDAO}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{AnyContent, Controller}
import play.api.Environment
import play.api.data.Form
import utils.AppMode
import utils.auth.DefaultEnv

import scala.concurrent.Future

@Singleton
class TicketController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  milestoneDAO: MilestoneDAO,
  ticketDAO: TicketDAO,
  eventBus: EventBus,
  val verifier: RecaptchaVerifier,
  implicit val appMode: AppMode,
  implicit val webJarAssets: WebJarAssets,
  implicit val environment: Environment
)(implicit widgetHelper: WidgetHelper)
extends Controller with I18nSupport {

  def view = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.ticket(TicketForm.form)))
  }

  def detail(uuid: String) = silhouette.UserAwareAction.async { implicit request =>
    ticketDAO.find(UUID.fromString(uuid)).flatMap {
      case None => Future.successful(Redirect(routes.ApplicationController.index()).flashing("error" -> "Ticket not found"))
      case Some(ticket) => Future.successful(Ok(views.html.ticketDetail(ticket)))
    }
  }

  def submit = silhouette.SecuredAction.async { implicit request =>
    if (appMode.isProd) {
      verifier.bindFromRequestAndVerify(TicketForm.form).flatMap { form =>
        formFoldHelper(form)
      }
    }
    else {
      formFoldHelper(TicketForm.form.bindFromRequest)
    }
  }

  def formFoldHelper(form: Form[TicketForm.Data])(implicit request: SecuredRequest[DefaultEnv, AnyContent]) = {
    form.fold(
      form => Future.successful(BadRequest(views.html.ticket(form))),
      data => {
        val ticket = Ticket(
          id = Some(UUID.randomUUID()),
          reporter = Some(request.identity.username),
          reporterName = request.identity.name,
          assignedTo = None,
          assignedToName = None,
          status = None,
          priority = None,
          title = Some(data.title),
          description = Some(data.description),
          resolution = None,
          media = None
        )
        ticketDAO.save(ticket).flatMap { t =>
          Future.successful(Ok(views.html.ticketDetail(t)))
        }
      }
    )
  }

}