package events

import com.mohiva.play.silhouette.api.SilhouetteEvent
import models.TokenInfo

case class ResetPasswordEvent(tokenInfo: TokenInfo, newPassword: String) extends SilhouetteEvent
