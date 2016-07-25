package modules

import actors._
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    bindActor[AccountServiceRelatedActor]("account-service-related-actor")
    //    bindActor[BatchTrainer]("batch-trainer")
    //    bindActor[Classifier]("classifier")
    //    bindActor[Director]("director")
    //    bindActor[CorpusInitializer]("corpus-initializer")
    //    bindActor[SimilarTicketFinder]("similar-ticket-finder")
  }

}
