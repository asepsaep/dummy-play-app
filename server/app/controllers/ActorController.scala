package controllers

import java.util.UUID
import javax.inject.{ Inject, Named, Singleton }

import actors.Classifier.{ ClassificationResult, Classify }
import akka.pattern._
import actors.Director
import actors.Director.{ BuildModel, GetClassifier }
import akka.actor.{ ActorRef, ActorSystem }
import akka.util.Timeout
import com.mohiva.play.silhouette.api.{ EventBus, Silhouette }
import models.LabeledTicket
import models.daos.TicketDAO
import org.apache.spark.SparkContext
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.{ Environment, Logger }
import play.api.mvc.{ Action, Controller }
import utils.AppMode
import utils.auth.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class ActorController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  ticketDAO: TicketDAO,
  sparkContext: SparkContext,
  system: ActorSystem,
  eventBus: EventBus,
  implicit val appMode: AppMode,
  implicit val webJarAssets: WebJarAssets,
  implicit val environment: Environment
) extends Controller with I18nSupport {

  val director = system.actorOf(Director.props(sparkContext), "director")
  implicit val timeout = Timeout(100 seconds)

  def build = Action.async { implicit request ⇒
    director ! BuildModel
    Future.successful(Ok("Done"))
  }

  def classify(id: String) = Action.async { implicit request ⇒
    ticketDAO.find(UUID.fromString(id)).flatMap {
      case None ⇒ Future.successful(Redirect(routes.ApplicationController.index()).flashing("error" → "Ticket not found"))
      case Some(ticket) ⇒ {
        Logger.debug("found ticket " + ticket.toString)
        val result = for {
          classifier ← (director ? GetClassifier).mapTo[ActorRef]
          classificationResult ← (classifier ? Classify(ticket)).map {
            case result: ClassificationResult ⇒ result
            case labeledTicket: LabeledTicket ⇒ labeledTicket
            case array: Array[LabeledTicket]  ⇒ array(0)
            case e: Any                       ⇒ throw new IllegalStateException("sum ting wong ==> " + e.getClass + " " + e.toString)
          }
        } yield classificationResult
        result.flatMap { res ⇒
          Future.successful(Ok(res.toString))
        }
      }
    }
  }

}
