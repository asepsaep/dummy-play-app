package events

import com.mohiva.play.silhouette.api.SilhouetteEvent
import models.TokenInfo

case class EmailConfirmedEvent(tokenInfo: TokenInfo) extends SilhouetteEvent