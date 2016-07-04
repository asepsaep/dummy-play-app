package classifier

import java.util.UUID

import actors.BatchTrainer.BatchTrainerModel
import models.{ LabeledTicket, Ticket, UnlabeledTicket }
import org.apache.spark.SparkContext
import org.apache.spark.ml.Transformer
import org.apache.spark.mllib.classification.LogisticRegressionModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ Row, SQLContext }

trait PredictorProxy {
  def predict(batchTrainerModel: Transformer, Ticket: Ticket): Array[LabeledTicket]
}

class Predictor(sparkContext: SparkContext) extends PredictorProxy {

  val sqlContext = new SQLContext(sparkContext)
  import sqlContext.implicits._

  override def predict(batchTrainerModel: Transformer, ticket: Ticket): Array[LabeledTicket] = {
    val testData = sqlContext.createDataFrame(Seq(ticket.toUnlabeledTicket)).toDF()
    batchTrainerModel
      .transform(testData)
      .select("description", "prediction_label")
      .collect()
      .map {
        case Row(description: String, predictionLabel: String) ⇒
          //          LabeledTicket(Some(UUID.fromString(id)), Some(description), Some(predictionLabel))
          LabeledTicket(Some(description), Some(predictionLabel))
      }

  }

}
