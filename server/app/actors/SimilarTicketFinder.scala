package actors

import actors.CorpusInitializer.LoadTicketSummaryFromDB
import actors.Director.BatchTrainingFinished
import actors.SimilarTicketFinder.{NormalizedTicket, NormalizedTicketsPair, TicketSimilarity}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import models.{LabeledTicket, Ticket, TicketSummary}
import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.NaiveBayes
import org.apache.spark.ml.evaluation.{BinaryClassificationEvaluator, MulticlassClassificationEvaluator}
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.ml.{Pipeline, PipelineModel, Transformer}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SparkSession}

object SimilarTicketFinder {

  def props(sparkContext: SparkContext, sparkSession: SparkSession, director: ActorRef, corpusInitializer: ActorRef, webSocketActor: ActorRef) = Props(new SimilarTicketFinder(sparkContext, sparkSession, director, corpusInitializer, webSocketActor))

  case class Similar(ticket: Ticket) extends Serializable
  case class FindSimilarity(ticket: Ticket) extends Serializable
  case class NormalizedTicket(id: Long, vector: Array[Double]) extends Serializable
  case class NormalizedTicketsPair(t1: NormalizedTicket, t2: NormalizedTicket) extends Serializable
  case class TicketSimilarity(id: Long, similarity: Double) extends Serializable
  case class TicketSimilarityResult(id: Long, thatId: Long, similarity: Double) extends Serializable

}

class SimilarTicketFinder(sparkContext: SparkContext, sparkSession: SparkSession, director: ActorRef, corpusInitializer: ActorRef, webSocketActor: ActorRef) extends Actor with ActorLogging {

  import SimilarTicketFinder._

  val sqlContext = sparkSession.sqlContext
  import sqlContext.implicits._

  val tokenizer = new Tokenizer().setInputCol("description").setOutputCol("words")
  val hashingTF = new HashingTF().setNumFeatures(20000).setInputCol("words").setOutputCol("tf_features")
//  val idf = new IDF().setMinDocFreq(2).setInputCol("tf_features").setOutputCol("tfidf_features")
  val normalizer = new Normalizer().setInputCol("tf_features").setOutputCol("norm_features")
  val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, normalizer))

  var model: PipelineModel = _
  var tickets: DataFrame = _

  override def receive: Receive = LoggingReceive {

    case InitTicketSummary =>
      log.info("Init ticket summary command from director")
      corpusInitializer ! LoadTicketSummaryFromDB

    case TicketSummaryCorpus(corpus) ⇒
      log.info("Receive corpus from corpus initializer")
      model = pipeline.fit(corpus) // (corpus.map(t ⇒ (t.id, t.description)).toDF("id", "description"))
      tickets = model.transform(corpus)
      tickets.persist()
      tickets.first()
      log.info("Done")

//    case Similar(ticket) ⇒
//      log.debug("Find similar ticket")
//      val originalSender = sender
//      log.debug("Create similarity actor handler")
//      val handler = context.actorOf(SimilarTicketHelper.props(sparkContext, sparkSession, originalSender, ticket))
//      log.debug("Get latest model from batch trainer")
//      corpusInitializer.tell(LoadTicketSummaryFromDB, handler)

    case Similar(ticket) ⇒
      log.info("Find ticket similarity")
      val mainTicket = sparkContext.parallelize(Seq(ticket.toTicketSummary)).toDF("id", "description")
      val transformedMainTicket = model.transform(mainTicket)
      val ticketInfo = transformedMainTicket.map { tMain =>
        NormalizedTicket(tMain.getAs[Long]("id"), tMain.getAs[SparseVector]("norm_features").toArray)
      }.first
      val vectorA = ticketInfo.vector
      val id = ticketInfo.id
      val indices = vectorA.indices
      val vectorASum = math.sqrt(vectorA.map(v ⇒ v * v).sum)
      val result = tickets.map { t =>
        val vectorB = t.getAs[SparseVector]("norm_features").toArray
        val nominator = (for (i ← indices) yield (vectorA(i) * vectorB(i))).sum
        val denominator =  vectorASum * math.sqrt(vectorB.map(v ⇒ v * v).sum)
        TicketSimilarityResult(id, t.getAs[Long]("id"), nominator / denominator)
      }.collect
      sender ! result

  }

}

object SimilarTicketHelper {
  def props(sparkContext: SparkContext, sparkSession: SparkSession, originalSender: ActorRef, ticket: Ticket) = Props(new SimilarTicketHelper(sparkContext, sparkSession, originalSender, ticket))
}

class SimilarTicketHelper(sparkContext: SparkContext, sparkSession: SparkSession, originalSender: ActorRef, ticket: Ticket) extends Actor with ActorLogging {

  import SimilarTicketHelper._
  import SimilarTicketFinder._

  val sqlContext = sparkSession.sqlContext
  import sqlContext.implicits._

  override def receive: Receive = LoggingReceive {

    case TicketSummaryCorpus(corpus) ⇒ {
      log.info("Inside FindSimilar(corpus) case")

      log.info("Getting dataframe...")
      val dfTickets = corpus.map(t ⇒ (t.id, t.description)).toDF("id", "description")

      log.info("Initiating transformers...")
      val tokenizer = new Tokenizer().setInputCol("description").setOutputCol("words")
      val hashingTF = new HashingTF().setNumFeatures(20000).setInputCol("words").setOutputCol("tf_features")
      val idf = new IDF().setInputCol("tf_features").setOutputCol("tfidf_features")
      val normalizer = new Normalizer().setInputCol("tfidf_features").setOutputCol("norm_features")

      log.info("Creating pipeline...")
      val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, idf, normalizer))

      log.info("Creating pipeline model...")
      val model = pipeline.fit(dfTickets)

      log.info("Transforming dataframe...")
      val transformedTickets = model.transform(dfTickets)

      log.info("Loading and transforming main ticket...")
      val mainTicket = sparkContext.parallelize(Seq(ticket.toTicketSummary)).toDF("id", "description")
      val transformedMainTicket = model.transform(mainTicket)

      val tMainInfo = transformedMainTicket.map { tMain =>
        NormalizedTicket(tMain.getAs[Long]("id"), tMain.getAs[SparseVector]("norm_features").toArray)
      }.first

      val vectorA = tMainInfo.vector
      val id = tMainInfo.id
      val indices = vectorA.indices
      val vectorASum = math.sqrt(vectorA.map(v ⇒ v * v).sum)

      val result = transformedTickets.map { t =>
        val vectorB = t.getAs[SparseVector]("norm_features").toArray
        val nominator = (for (i ← indices) yield (vectorA(i) * vectorB(i))).sum
        val denominator =  vectorASum * math.sqrt(vectorB.map(v ⇒ v * v).sum)
        TicketSimilarityResult(id, t.getAs[Long]("id"), nominator / denominator)
      }.collect

      log.info("Sending result to original sender...")
      originalSender ! result

    }

  }


}