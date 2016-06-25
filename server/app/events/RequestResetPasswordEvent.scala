package events

import com.mohiva.play.silhouette.api.SilhouetteEvent
import models.Account

case class RequestResetPasswordEvent(account: Account) extends SilhouetteEvent