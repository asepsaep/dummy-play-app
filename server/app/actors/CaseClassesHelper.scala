package actors

import models.{ LabeledTicket, TicketSummary }
import org.apache.spark.rdd.RDD

case class Train(corpus: RDD[LabeledTicket])

case class FindSimilar(corpus: RDD[TicketSummary])

case class TicketSummaryCorpus(corpus: RDD[TicketSummary])