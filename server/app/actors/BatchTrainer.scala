package actors

import actors.Director.BatchTrainingFinished
import akka.actor.Actor.Receive
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.event.LoggingReceive
import models.{ LabeledTicket, Ticket }
import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.NaiveBayes
import org.apache.spark.ml.evaluation.{ BinaryClassificationEvaluator, MulticlassClassificationEvaluator }
import org.apache.spark.ml.feature.{ HashingTF, IndexToString, StringIndexer, Tokenizer }
import org.apache.spark.ml.tuning.{ CrossValidator, ParamGridBuilder }
import org.apache.spark.ml.{ Pipeline, Transformer }
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ DataFrame, SQLContext }

object BatchTrainer {

  def props(sparkContext: SparkContext, director: ActorRef) = Props(new BatchTrainer(sparkContext, director))

  case class BatchTrainerModel(model: Option[Transformer])

  case class BatchFeatures(features: Option[RDD[(String, Vector)]])

}

trait BatchTrainerProxy extends Actor

class BatchTrainer(sparkContext: SparkContext, director: ActorRef) extends Actor with ActorLogging with BatchTrainerProxy {

  import BatchTrainer._

  var model: Option[Transformer] = None

  val sqlContext = new SQLContext(sparkContext)

  import sqlContext.implicits._

  override def receive: Receive = LoggingReceive {

    case Train(corpus: RDD[LabeledTicket]) ⇒ {
      log.debug("Received Train message with ticket corpus")
      log.info("Start batch training")

      val data: DataFrame = corpus.map(t ⇒ (t.description, t.assignedTo)).toDF("description", "assigned_to")
      log.debug("Corpus mapped")

      val indexer = new StringIndexer().setInputCol("assigned_to").setOutputCol("label").fit(data)
      val tokenizer = new Tokenizer().setInputCol("description").setOutputCol("words")
      val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol(tokenizer.getOutputCol).setOutputCol("features")
      val nb = new NaiveBayes()
      val labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("prediction_label").setLabels(indexer.labels)
      val pipeline = new Pipeline().setStages(Array(indexer, tokenizer, hashingTF, nb, labelConverter))
      val paramGrid = new ParamGridBuilder()
        .addGrid(hashingTF.numFeatures, Array(1000))
        .addGrid(nb.smoothing, Array(1.0))
        .build()
      val cv = new CrossValidator()
        .setEstimator(pipeline)
        .setEvaluator(new MulticlassClassificationEvaluator)
        .setEstimatorParamMaps(paramGrid)
        .setNumFolds(3)

      log.debug("calling cv.fit")
      model = Some[Transformer](cv.fit(data).bestModel)

      log.info("Batch training finished")

      director ! BatchTrainingFinished

    }

    case GetLatestModel ⇒ {
      log.debug("Received GetLatestModel message")
      sender ! BatchTrainerModel(model)
      log.debug(s"Returned model $model")
    }

  }

}
