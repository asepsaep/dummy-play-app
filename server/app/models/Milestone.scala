package models

import java.time.OffsetDateTime
import java.util.UUID

case class Milestone(
  id: Option[UUID],
  ticketId: Option[UUID],
  datetime: OffsetDateTime = OffsetDateTime.now(),
  description: Option[String],
  milestoneReporter: Option[String],
  milestoneReporterName: Option[String]
)
