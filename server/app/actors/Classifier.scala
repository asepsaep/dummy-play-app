package actors

import actors.BatchTrainer.BatchTrainerModel
import akka.actor.Actor.Receive
import classifier.PredictorProxy
import akka.actor._
import akka.event.LoggingReceive
import models.{LabeledTicket, Ticket}
import org.apache.spark.SparkContext
import org.apache.spark.ml.{PipelineModel, Transformer}
import org.apache.spark.mllib.classification.LogisticRegressionModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SparkSession}

import scala.concurrent.duration._

object Classifier {

  def props(sparkContext: SparkContext, sparkSession: SparkSession, batchTrainer: ActorRef, predictor: PredictorProxy) =
    Props(new Classifier(sparkContext, sparkSession, batchTrainer, predictor))

  case class Classify(ticket: Ticket)
  case class ClassificationResult(batchModelResult: Seq[LabeledTicket])

}

class Classifier(sparkContext: SparkContext, sparkSession: SparkSession, batchTrainer: ActorRef, predictor: PredictorProxy) extends Actor with ActorLogging {

  import Classifier._

  val sqlContext = sparkSession.sqlContext
  import sqlContext.implicits._

  override def receive: Receive = LoggingReceive {

    case Classify(ticket) ⇒
      log.debug("Classifying.....")
      val originalSender = sender
      log.debug("Create classifier handler")
      val handler = context.actorOf(ClassifierHandler.props(batchTrainer, originalSender, sparkContext, sparkSession, predictor, ticket))
      log.debug("Get latest model from batch trainer")
      batchTrainer.tell(GetLatestModel, handler)

  }

}

object ClassifierHandler {

  def props(batchTrainer: ActorRef, originalSender: ActorRef, sparkContext: SparkContext, sparkSession: SparkSession, predictor: PredictorProxy, ticket: Ticket) =
    Props(new ClassifierHandler(batchTrainer, originalSender, sparkContext, sparkSession, predictor, ticket))

}

class ClassifierHandler(batchTrainer: ActorRef, originalSender: ActorRef, sparkContext: SparkContext, sparkSession: SparkSession, predictor: PredictorProxy, ticket: Ticket) extends Actor with ActorLogging {

  import actors.ClassifierHandler._

  val sqlContext = sparkSession.sqlContext
  import sqlContext.implicits._

  var batchTrainerModel: Option[Transformer] = None

  override def receive: Receive = LoggingReceive {

    case BatchTrainerModel(model) ⇒
      log.debug(s"Received batch trainer model: $model")
      batchTrainerModel = model
      predict()

  }

  def predict() = batchTrainerModel match {

    case Some(batchModelTransformer) ⇒
      log.debug("Calling predictor.predict() ...")
      val batchModelResult = predictor.predict(batchModelTransformer, ticket)
      originalSender ! batchModelResult
      context.stop(self)

    case _ ⇒

  }

}

