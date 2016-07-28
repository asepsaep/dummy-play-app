package actors

import models.{LabeledTicket, TicketSummary}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Dataset

case class Train(corpus: Dataset[LabeledTicket])

case class FindSimilar(corpus: RDD[TicketSummary])

case class TicketSummaryCorpus(corpus: Dataset[TicketSummary])

case object InitTicketSummary