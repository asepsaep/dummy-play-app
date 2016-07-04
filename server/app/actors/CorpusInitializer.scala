package actors

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.event.LoggingReceive
import models.LabeledTicket
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.{ Duration, StreamingContext }

object CorpusInitializer {

  def props(sparkContext: SparkContext, batchTrainer: ActorRef) = Props(new CorpusInitializer(sparkContext, batchTrainer))

  case object InitFromStream

  case object LoadFromFs

  case object LoadFromDb

  case object Finish

}

class CorpusInitializer(sparkContext: SparkContext, batchTrainer: ActorRef) extends Actor with ActorLogging {

  import CorpusInitializer._
  val sqlContext = new SQLContext(sparkContext)
  import sqlContext.implicits._

  override def receive: Receive = LoggingReceive {

    case LoadFromDb ⇒ {
      log.debug("Load from db....")
      val opts = Map("url" → "jdbc:postgresql://localhost:5432/play?user=asep&password=passasus0", "dbtable" → "ticket")
      log.debug(s"Option = $opts")
      val df = sqlContext.read.format("jdbc").options(opts).load()
      log.debug("Data frame created." + df.printSchema() + "\n" + df.first())
      val data = df.map {
        case row ⇒
          //          LabeledTicket(Some(UUID.fromString(row.getAs[String]("id"))), Some(row.getAs[String]("description")), Some(row.getAs[String]("assigned_to")))
          LabeledTicket(Some(row.getAs[String]("description")), Some(row.getAs[String]("assigned_to")))
      }
      log.debug("Telling batch trainer...")
      batchTrainer ! Train(data)

    }

  }

}

