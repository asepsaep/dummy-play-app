package controllers

import javax.inject.{Inject, Named, Singleton}

import actors.Classifier.{ClassificationResult, Classify}
import akka.pattern._
import actors.{Director, WebSocketActor}
import actors.Director._
import actors.SimilarTicketFinder.{NormalizedTicket, NormalizedTicketsPair, Similar, TicketSimilarity}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.util.Timeout
import com.mohiva.play.silhouette.api.{EventBus, Silhouette}
import models.{LabeledTicket, Ticket, TicketSummary}
import models.daos.{AccountDAO, TicketDAO}
import org.apache.spark.SparkContext
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.streams.ActorFlow
import play.api.{Environment, Logger}
import play.api.mvc.{Action, Controller, WebSocket}
import utils.AppMode
import utils.auth.DefaultEnv

import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class ActorController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  ticketDAO: TicketDAO,
  accountDAO: AccountDAO,
  sparkContext: SparkContext,
  eventBus: EventBus,
  implicit val system: ActorSystem,
  implicit val materializer: Materializer,
  implicit val appMode: AppMode,
  implicit val webJarAssets: WebJarAssets,
  implicit val environment: Environment
) extends Controller with I18nSupport {

  val director = system.actorOf(Director.props(sparkContext), "director")
  implicit val timeout = Timeout(600 seconds) // Ten minutes

  def socket = WebSocket.accept[String, String] { request ⇒
    ActorFlow.actorRef { out: ActorRef ⇒
      val props = WebSocketActor.props(out, sparkContext, director)
      director ! CreateWebSocketActor(props)
      props
    }
  }

  def build = Action.async { implicit request ⇒
    Future.successful(Ok(views.html.build()))
  }

  def classify(id: Long) = Action.async { implicit request ⇒
    ticketDAO.find(id).flatMap {
      case None ⇒ Future.successful(Redirect(routes.ApplicationController.index()).flashing("error" → "Ticket not found"))
      case Some(ticket) ⇒ {
        Logger.debug("found ticket " + ticket.toString)
        val result = for {
          classifier ← (director ? GetClassifier).mapTo[ActorRef]
          classificationResult ← (classifier ? Classify(ticket)).map {
            case labeledTicket: Ticket ⇒ labeledTicket
            case array: Array[Ticket]  ⇒ array(0)
          }
        } yield classificationResult
        result.flatMap { ticket ⇒
          accountDAO.find(ticket.assignedTo.getOrElse("")).flatMap {
            case None          ⇒ Future.successful(Ok(views.html.ticketDetail(ticket)))
            case Some(account) ⇒ Future.successful(Ok(views.html.ticketDetail(ticket.copy(assignedToName = account.name))))
          }
        }
      }
    }
  }

  def similar(id: Long) = Action.async { implicit request ⇒
    ticketDAO.find(id).flatMap {
      case None ⇒ Future.successful(Redirect(routes.ApplicationController.index()).flashing("error" → "Ticket not found"))
      case Some(ticket) ⇒ {
        Logger.debug("found ticket " + ticket.toString)
        val result = for {
          similarityFinder ← (director ? GetTicketSimilarity).mapTo[ActorRef]
          similarityResult ← (similarityFinder ? Similar(ticket)).map {
            case array: Array[NormalizedTicketsPair] ⇒ array
            case a @ e                              ⇒
              Logger.info(a.getClass.toString)
              Array[NormalizedTicketsPair]()
          }
        } yield similarityResult
        result.flatMap { content ⇒
          val similarTicket = content.map { t =>
            TicketSimilarity(t.t1.id, cosineSimilarity(t.t1.vector, t.t2.vector))
          }.sortBy(t => -t.similarity)
          Future.successful(Ok(similarTicket.take(11).mkString("\n")))
        }
      }
    }
  }

  private def cosineSimilarity(vectorA: Array[Double], vectorB: Array[Double]): Double = {
    val nominator = (for (i ← vectorA.indices) yield (vectorA(i) * vectorB(i))).sum
    val denominator = math.sqrt(vectorA.map(v ⇒ v * v).sum) * math.sqrt(vectorB.map(v ⇒ v * v).sum)
    val cosineSimilartyResult = nominator / denominator
//    Logger.debug(s"Nominator / Denominator = $nominator / $denominator = $cosineSimilartyResult")
    cosineSimilartyResult
  }

}
