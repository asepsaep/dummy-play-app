package actors

import actors.Director.BuildModel
import akka.actor.Actor.Receive
import akka.actor._
import org.apache.spark.SparkContext

object WebSocketActor {
  def props(out: ActorRef, sparkContext: SparkContext, director: ActorRef) = Props(new WebSocketActor(out, sparkContext, director))
}

class WebSocketActor(out: ActorRef, sparkContext: SparkContext, director: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {

    case message: String ⇒ message match {
      case "build"     ⇒ director ! BuildModel
      case any: String ⇒ out ! any
    }

  }

}
