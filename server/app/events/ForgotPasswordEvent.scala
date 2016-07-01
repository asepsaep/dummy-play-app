package events

import com.mohiva.play.silhouette.api.SilhouetteEvent
import models.Account

case class ForgotPasswordEvent(account: Account) extends SilhouetteEvent