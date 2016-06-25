package events

import com.mohiva.play.silhouette.api.SilhouetteEvent
import models.Account

//case class CredentialsSignUpEvent(account: Account) extends SilhouetteEvent

case class CredentialsSignUpEvent(account: Account) extends SilhouetteEvent