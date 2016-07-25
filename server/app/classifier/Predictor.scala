package classifier

import java.util.UUID
import javax.inject.Inject

import actors.BatchTrainer.BatchTrainerModel
import actors.SimilarTicketFinder.TicketSimilarity
import models.daos.AccountDAO
import models.{ LabeledTicket, Ticket }
import org.apache.spark.SparkContext
import org.apache.spark.ml.Transformer
import org.apache.spark.mllib.classification.LogisticRegressionModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ Row, SQLContext }

trait PredictorProxy {
  def predict(batchTrainerModel: Transformer, Ticket: Ticket): Array[Ticket]
}

class Predictor @Inject() (sparkContext: SparkContext) extends PredictorProxy {

  val sqlContext = new SQLContext(sparkContext)
  import sqlContext.implicits._

  override def predict(batchTrainerModel: Transformer, ticket: Ticket): Array[Ticket] = {
    val testData = sqlContext.createDataFrame(Seq(ticket.toTicketSummary)).toDF()
    batchTrainerModel
      .transform(testData)
      .select("description", "prediction_label")
      .collect()
      .map {
        case Row(description: String, predictionLabel: String) ⇒
          ticket.copy(assignedTo = Some(predictionLabel))
      }

  }

}
