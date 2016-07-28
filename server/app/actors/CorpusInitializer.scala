package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import com.typesafe.config.{Config, ConfigFactory}
import models.{LabeledTicket, Ticket, TicketSummary}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.streaming.{Duration, StreamingContext}
import net.ceedubs.ficus.Ficus._

object CorpusInitializer {

  def props(sparkContext: SparkContext, sparkSession: SparkSession, batchTrainer: ActorRef) = Props(new CorpusInitializer(sparkContext, sparkSession, batchTrainer))

  case object InitFromStream
  case object LoadTicketSummaryFromDB
  case object LoadLabeledTicketFromDB
  case object Finish

}

class CorpusInitializer(sparkContext: SparkContext, sparkSession: SparkSession, batchTrainer: ActorRef) extends Actor with ActorLogging {

  import CorpusInitializer._
  val sqlContext = sparkSession.sqlContext
  import sqlContext.implicits._

  override def receive: Receive = LoggingReceive {

    case LoadLabeledTicketFromDB ⇒ {
      val config: Config = ConfigFactory.load()
      val dbUrl = config.as[String]("db.ticket.url")
      val dbTable = config.as[String]("db.ticket.labeledTicketTable")
      val dbUser = config.as[String]("db.ticket.user")
      val dbPassword = config.as[String]("db.ticket.password")

      log.debug("Load from db....")
      val opts = Map("url" → s"$dbUrl?user=$dbUser&password=$dbPassword", "dbtable" → dbTable, "driver" -> "org.postgresql.Driver")
      log.debug(s"Option = $opts")
      val df = sqlContext.read.format("jdbc").options(opts).load()
      log.debug("Data frame created." + df.printSchema() + "\n" + df.first())
      val data = df.map {
        case row ⇒
          LabeledTicket(Some(row.getAs[String]("description")), Some(row.getAs[String]("assigned_to")))
      }
      log.debug("Telling sender...")
      sender ! Train(data)

    }

    case LoadTicketSummaryFromDB ⇒ {
      val config: Config = ConfigFactory.load()
      val dbUrl = config.as[String]("db.ticket.url")
      val dbTable = config.as[String]("db.ticket.ticketTable")
      val dbUser = config.as[String]("db.ticket.user")
      val dbPassword = config.as[String]("db.ticket.password")

      log.debug("Load from db....")
      val opts = Map("url" → s"$dbUrl?user=$dbUser&password=$dbPassword", "dbtable" → dbTable)
      log.debug(s"Option = $opts")
      val df = sqlContext.read.format("jdbc").options(opts).load()
      log.debug("Data frame created." + df.printSchema() + "\n" + df.first())
      val data = df.map {
        case row ⇒
          TicketSummary(Some(row.getAs[Long]("id")), Some(row.getAs[String]("description")))
      }
      log.debug("Telling sender...")
      sender ! TicketSummaryCorpus(data)

    }

  }

}

