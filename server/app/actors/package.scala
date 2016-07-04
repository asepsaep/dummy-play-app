import models.LabeledTicket
import org.apache.spark.rdd.RDD

package object actors {

  case object GetLatestModel

  case class Train(corpus: RDD[LabeledTicket])

  case object Subscribe

  case object Unsubscribe

}
