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
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.ml.{Pipeline, Transformer}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.{DenseVector, SparseVector, Vector}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

object SimilarTicketFinder {

  def props(sparkContext: SparkContext, director: ActorRef, corpusInitializer: ActorRef, webSocketActor: ActorRef) = Props(new SimilarTicketFinder(sparkContext, director, corpusInitializer, webSocketActor))

  case class Similar(ticket: Ticket) extends Serializable
  case class NormalizedTicket(id: Long, vector: Array[Double]) extends Serializable
  case class NormalizedTicketsPair(t1: NormalizedTicket, t2: NormalizedTicket) extends Serializable
  case class TicketSimilarity(id: Long, similarity: Double) extends Serializable

}

class SimilarTicketFinder(sparkContext: SparkContext, director: ActorRef, corpusInitializer: ActorRef, webSocketActor: ActorRef) extends Actor with ActorLogging {

  val sqlContext = new SQLContext(sparkContext)
  import sqlContext.implicits._
  import SimilarTicketFinder._

  override def receive: Receive = LoggingReceive {

    case Similar(ticket) ⇒
      log.debug("Find similar.....")
      val originalSender = sender
      log.debug("Create similar finder helper")
      val handler = context.actorOf(SimilarTicketHelper.props(sparkContext, originalSender, ticket))
      log.debug("Get latest model from batch trainer")
      corpusInitializer.tell(LoadTicketSummaryFromDB, handler)

  }

}

object SimilarTicketHelper {
  def props(sparkContext: SparkContext, originalSender: ActorRef, ticket: Ticket) = Props(new SimilarTicketHelper(sparkContext, originalSender, ticket))
}

class SimilarTicketHelper(sparkContext: SparkContext, originalSender: ActorRef, ticket: Ticket) extends Actor with ActorLogging {

  import SimilarTicketHelper._
  val sqlContext = new SQLContext(sparkContext)
  import sqlContext.implicits._

  override def receive: Receive = LoggingReceive {

    case FindSimilar(corpus) ⇒ {
      log.info("Inside FindSimilar(corpus) case")
      corpus.cache()

      log.info("Getting dataframe...")
      val dfTickets = corpus.map(t ⇒ (t.id, t.description)).toDF("id", "description")

      log.info("Initiating transformers...")
      val tokenizer = new Tokenizer().setInputCol("description").setOutputCol("words")
      val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol("words").setOutputCol("raw_features")
      val normalizer = new Normalizer().setInputCol("raw_features").setOutputCol("norm_features")
//      val idf = new IDF().setInputCol("norm_features").setOutputCol("features")

      log.info("Creating pipeline...")
      val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, normalizer))

      log.info("Loading and transforming main ticket...")
      val mainTicket = sparkContext.parallelize(Seq(ticket.toTicketSummary)).toDF("id", "description")
      val transformedMainTicket = pipeline.fit(mainTicket).transform(mainTicket)

      log.info("Transforming dataframe...")
      val transformedTickets = pipeline.fit(dfTickets).transform(dfTickets)

      log.info("Mapping transformed dataframe...")
      val theTicket = transformedMainTicket.map(t ⇒ NormalizedTicket(t.getAs[Long]("id"), t.getAs[SparseVector]("norm_features").toArray))
      val tickets = transformedTickets.map(t ⇒ NormalizedTicket(t.getAs[Long]("id"), t.getAs[SparseVector]("norm_features").toArray))

      log.info("Combining ticket dataframe with main ticket...")
      val combinedTickets = tickets.cartesian(theTicket)
      combinedTickets.cache()

      log.info("Collectiong result...")
      val result = combinedTickets.map { case (r1, r2) => NormalizedTicketsPair(r1, r2) }.collect()

//      combinedTickets.cache()

//      originalSender ! combinedTickets.collect()

//      log.info("Calculating cosine similarity result...")
//      val mappedTickets = combinedTickets.map { case (r1, r2) =>
//        val nominator = (for (i ← r1.vector.indices) yield (r1.vector(i) * r2.vector(i))).sum
//        val denominator = math.sqrt(r1.vector.map(v ⇒ v * v).sum) * math.sqrt(r2.vector.map(v ⇒ v * v).sum)
//        val cosineSimilartyResult = nominator / denominator
//        TicketSimilarity(r1.id, cosineSimilartyResult)
//      }

//      log.info("Sorting ticket similarity result...")
//      val sortedMappedTicket = mappedTickets.sortBy(f = (t: TicketSimilarity) ⇒ t.similarity, ascending = false)
//
//      log.info("Take top 10")
//      val topSimilar = sortedMappedTicket.take(11)
//
//      Alternatively
//      val topSimilar = mappedTickets.top(10)(Ordering.by(t => t.similarity))
//
//      log.info(topSimilar.mkString("\n"))
//      log.info(topSimilar.map(_.toString).mkString("\n"))

      log.info("Sending result to original sender...")
      originalSender ! result

    }

  }

//  private def cosineSimilarity(vectorA: Array[Double], vectorB: Array[Double]): Double = {
//    val nominator = (for (i ← vectorA.indices) yield (vectorA(i) * vectorB(i))).sum
//    val denominator = math.sqrt(vectorA.map(v ⇒ v * v).sum) * math.sqrt(vectorB.map(v ⇒ v * v).sum)
//    val cosineSimilartyResult = nominator / denominator
//    cosineSimilartyResult
//  }

}