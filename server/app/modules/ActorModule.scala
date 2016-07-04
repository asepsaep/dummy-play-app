package modules

import actors.{ AccountServiceRelatedActor, Director }
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    bindActor[AccountServiceRelatedActor]("account-service-related-actor")
  }

}
