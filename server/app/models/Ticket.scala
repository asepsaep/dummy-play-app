package models

import java.time.OffsetDateTime
import java.util.UUID
import utils.TextCleaning

case class Ticket(
  id: Option[UUID],
  reporter: Option[String],
  reporterName: Option[String],
  assignedTo: Option[String],
  assignedToName: Option[String],
  cc: List[String] = List.empty,
  createdAt: OffsetDateTime = OffsetDateTime.now(),
  status: Option[String],
  priority: Option[String],
  title: Option[String],
  description: Option[String],
  resolution: Option[String],
  attachment: List[String] = List.empty,
  media: Option[String]
) extends Serializable {

  //  def toUnlabeledTicket = UnlabeledTicket(id, description.map(TextCleaning.clean))
  //  def toLabeledTicket = LabeledTicket(id, description, assignedTo)
  def toUnlabeledTicket = UnlabeledTicket(description.map(TextCleaning.clean))
  def toLabeledTicket = LabeledTicket(description, assignedTo)

}

case class UnlabeledTicket(
  //  id: Option[UUID],
  description: Option[String]
) extends Serializable

case class LabeledTicket(
  //  id: Option[UUID],
  description: Option[String],
  assignedTo: Option[String]
) extends Serializable