package actors

import actors.BatchTrainer.BatchTrainerModel
import actors.CorpusInitializer.{ LoadTicketSummaryFromDB, LoadLabeledTicketFromDB }
import akka.actor.Actor.Receive
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.event.LoggingReceive
import classifier.Predictor
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration._

object Director {

  def props(sparkContext: SparkContext) = Props(new Director(sparkContext))

  case object BuildModel
  case object GetClassifier
  case object GetTicketSimilarity
  case object BatchTrainingFinished
  case class CreateWebSocketActor(props: Props)

}

class Director(sparkContext: SparkContext) extends Actor with ActorLogging {

  import Director._

  var webSocketActor: ActorRef = _
  var batchTrainer: ActorRef = _
  var classifier: ActorRef = _
  var corpusInitializer: ActorRef = _
  var similarityFinder: ActorRef = _

  val predictor = new Predictor(sparkContext)

  override def receive: Receive = LoggingReceive {

    case GetClassifier         ⇒ sender ! classifier
    case GetTicketSimilarity   ⇒ sender ! similarityFinder
    case BatchTrainingFinished ⇒ batchTrainer ! GetLatestModel
    case BuildModel            ⇒ corpusInitializer.tell(LoadLabeledTicketFromDB, batchTrainer)

    case CreateWebSocketActor(props) ⇒
      webSocketActor = context.actorOf(props)
      batchTrainer = context.actorOf(BatchTrainer.props(sparkContext, self, webSocketActor), "batch-trainer")
      corpusInitializer = context.actorOf(CorpusInitializer.props(sparkContext, batchTrainer), "corpus-initializer")
      classifier = context.actorOf(Classifier.props(sparkContext, batchTrainer, predictor), "classifier")
      similarityFinder = context.actorOf(SimilarTicketFinder.props(sparkContext, self, corpusInitializer, webSocketActor), "similarity-finder")

    case BatchTrainerModel(model) ⇒ log.info("Got BatchTrainerModel")
    case undefined                ⇒ log.info(s"Unexpected message $undefined")

  }

}
