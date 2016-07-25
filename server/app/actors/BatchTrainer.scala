package actors

import actors.Director.BatchTrainingFinished
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.event.LoggingReceive
import models.{ LabeledTicket, Ticket }
import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.NaiveBayes
import org.apache.spark.ml.evaluation.{ BinaryClassificationEvaluator, MulticlassClassificationEvaluator }
import org.apache.spark.ml.feature._
import org.apache.spark.ml.tuning.{ CrossValidator, ParamGridBuilder }
import org.apache.spark.ml.{ Pipeline, Transformer }
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ DataFrame, Row, SQLContext }

object BatchTrainer {

  def props(sparkContext: SparkContext, director: ActorRef, webSocketActor: ActorRef) = Props(new BatchTrainer(sparkContext, director, webSocketActor))

  case class BatchTrainerModel(model: Option[Transformer])
  case class BatchFeatures(features: Option[RDD[(String, Vector)]])

}

trait BatchTrainerProxy extends Actor

class BatchTrainer(sparkContext: SparkContext, director: ActorRef, ws: ActorRef) extends Actor with ActorLogging with BatchTrainerProxy {

  import BatchTrainer._

  var model: Option[Transformer] = None

  val sqlContext = new SQLContext(sparkContext)

  import sqlContext.implicits._

  override def receive: Receive = LoggingReceive {

    case Train(corpus: RDD[LabeledTicket]) ⇒ {
      log.debug("Received Train message with ticket corpus")
      ws ! "Received Train message with ticket corpus"
      log.info("Start batch training")
      ws ! "Start batch training"

      val data: DataFrame = corpus.map(t ⇒ (t.description, t.assignedTo)).toDF("description", "assigned_to")
      log.debug("Corpus mapped")
      ws ! "Corpus mapped"

      val indexer = new StringIndexer().setInputCol("assigned_to").setOutputCol("label").fit(data)
      val tokenizer = new Tokenizer().setInputCol("description").setOutputCol("words")
      //      val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol("words").setOutputCol("raw_features")
      val hashingTF = new HashingTF().setInputCol("words").setOutputCol("raw_features")
      val idf = new IDF().setMinDocFreq(2).setInputCol("raw_features").setOutputCol("features")
      val nb = new NaiveBayes()
      val labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("prediction_label").setLabels(indexer.labels)
      val pipeline = new Pipeline().setStages(Array(indexer, tokenizer, hashingTF, idf, nb, labelConverter))
      val paramGrid = new ParamGridBuilder()
        .addGrid(hashingTF.numFeatures, Array(1000))
        .addGrid(nb.smoothing, Array(1.0))
        .build()
      val cv = new CrossValidator()
        .setEstimator(pipeline)
        .setEvaluator(new MulticlassClassificationEvaluator)
        .setEstimatorParamMaps(paramGrid)
        .setNumFolds(3)

      val result = cv.fit(data)
      val bestModel = result.bestModel
      model = Some[Transformer](result.bestModel)

      log.info("Batch training finished")
      ws ! "Batch training finished"

      director ! BatchTrainingFinished

      log.info("Start calculating evaluation metrics")
      ws ! "Start calculating evaluation metrics"

      val predictionAndLabels = bestModel
        .transform(data)
        .select("prediction", "label")
        .map { case Row(prediction: Double, label: Double) ⇒ (prediction, label) }

      val metrics = new MulticlassMetrics(predictionAndLabels)

      log.info(printMetrics(metrics).replace("<br>", "\n"))
      ws ! (printMetrics(metrics))

      val evaluator = new MulticlassClassificationEvaluator()
        .setLabelCol("assigned_to")
        .setPredictionCol("prediction_label")
        .setMetricName("precision")

      val prediction = bestModel
        .transform(data)
        .select("assigned_to", "prediction_label")
        .map { case Row(assignedTo: String, predictionLabel: String) ⇒ if (assignedTo == predictionLabel) 1 else 0 }
        .reduce(_ + _)

      val accuracy = (prediction.toDouble / data.count()) * 100

      log.info(accuracy + "%")
      ws ! ("Accuracy = " + accuracy + "%")
      log.info("Evaluation metrics calculation finished")
      ws ! "Evaluation metrics calculation finished"
    }

    case GetLatestModel ⇒ {
      log.debug("Received GetLatestModel message")
      sender ! BatchTrainerModel(model)
      log.debug(s"Returned model $model")
    }

  }

  private def printMetrics(metrics: MulticlassMetrics): String = {
    "fMeasure = " + metrics.fMeasure +
      "<br>precision = " + metrics.precision +
      "<br>weightedFMeasure = " + metrics.weightedFMeasure +
      "<br>weightedPrecision = " + metrics.weightedPrecision +
      "<br>weightedTruePositiveRate = " + metrics.weightedTruePositiveRate +
      "<br>weightedFalsePositiveRate = " + metrics.weightedFalsePositiveRate +
      "<br>recall = " + metrics.recall
  }

}
