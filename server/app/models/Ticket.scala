package models

import java.time.OffsetDateTime
import java.util.UUID

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
)