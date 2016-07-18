package models

import java.time.OffsetDateTime
import utils.TextCleaning

case class Ticket(
  id: Option[Long],
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

  def toTicketSummary = TicketSummary(id, description.map(TextCleaning.clean))
  def toLabeledTicket = LabeledTicket(description.map(TextCleaning.clean), assignedTo)

}

case class TicketSummary(
  id: Option[Long],
  description: Option[String]
) extends Serializable

case class LabeledTicket(
  description: Option[String],
  assignedTo: Option[String]
) extends Serializable