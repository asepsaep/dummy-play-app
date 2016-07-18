package models

import java.time.OffsetDateTime

case class Milestone(
  id: Option[Long],
  ticketId: Option[Long],
  datetime: OffsetDateTime = OffsetDateTime.now(),
  description: Option[String],
  milestoneReporter: Option[String],
  milestoneReporterName: Option[String]
)
