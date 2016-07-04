package actors

import actors.BatchTrainer.BatchTrainerModel
import actors.CorpusInitializer.LoadFromDb
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

  case object BatchTrainingFinished

}

class Director(sparkContext: SparkContext) extends Actor with ActorLogging {

  import Director._

  val batchTrainer = context.actorOf(BatchTrainer.props(sparkContext, self), "batch-trainer")

  val predictor = new Predictor(sparkContext)

  val classifier = context.actorOf(Classifier.props(sparkContext, batchTrainer, predictor), "classifier")

  val corpusInitializer = context.actorOf(CorpusInitializer.props(sparkContext, batchTrainer), "corpus-initializer")

  override def receive: Receive = LoggingReceive {

    case GetClassifier ⇒ {
      log.info("Get Classifier...")
      sender ! classifier
    }

    case BatchTrainingFinished ⇒ {
      batchTrainer ! GetLatestModel
    }

    case BuildModel ⇒ {
      corpusInitializer ! LoadFromDb
    }

    case BatchTrainerModel(model) ⇒ {
      log.info("Got BatchTrainerModel")
    }

    case undefined ⇒ {
      log.info(s"Unexpected message $undefined")
    }

  }

}
