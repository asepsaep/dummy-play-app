package models

import java.time.OffsetDateTime
import java.util.UUID

import modules.MyPostgresDriver.api._

class AccountTable(tag: Tag) extends Table[Account](tag, "account") {

  def id = column[Long]("id", O.AutoInc)
  def username = column[String]("username", O.PrimaryKey)
  def name = column[String]("name")
  def email = column[String]("email")
  def userType = column[String]("user_type")
  def userParent = column[String]("user_parent")
  def active = column[Boolean]("active")
  def avatarUrl = column[String]("avatar_url")
  def createdAt = column[OffsetDateTime]("created_at")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")

  override def * = (
    id.?,
    username,
    name.?,
    email.?,
    userType.?,
    userParent.?,
    active,
    avatarUrl.?,
    createdAt,
    providerId,
    providerKey
  ) <> (Account.tupled, Account.unapply _)

}

class PasswordInfoTable(tag: Tag) extends Table[DBPasswordInfo](tag, "password_info") {

  def hasher = column[String]("hasher")
  def password = column[String]("password")
  def salt = column[String]("salt")
  def accountUsername = column[String]("account_username")

  override def * = (hasher, password, salt.?, accountUsername) <> ((DBPasswordInfo.apply _).tupled, DBPasswordInfo.unapply _)

}

class OAuth1InfoTable(tag: Tag) extends Table[DBOAuth1Info](tag, "oauth1_info") {

  def token = column[String]("token")
  def secret = column[String]("secret")
  def accountUsername = column[String]("account_username")

  override def * = (token, secret, accountUsername) <> ((DBOAuth1Info.apply _).tupled, DBOAuth1Info.unapply _)

}

class OAuth2InfoTable(tag: Tag) extends Table[DBOAuth2Info](tag, "oauth2_info") {

  def accessToken = column[String]("access_token")
  def tokenType = column[String]("token_type")
  def expiresIn = column[Int]("expires_in")
  def refreshToken = column[String]("refresh_token")
  def params = column[Map[String, String]]("params")
  def accountUsername = column[String]("account_username")

  override def * = (
    accessToken,
    tokenType.?,
    expiresIn.?,
    refreshToken.?,
    params.?,
    accountUsername
  ) <> ((DBOAuth2Info.apply _).tupled, DBOAuth2Info.unapply _)

}

class TokenInfoTable(tag: Tag) extends Table[TokenInfo](tag, "token_info") {

  def token = column[String]("token", O.PrimaryKey)
  def email = column[String]("email")
  def expiresAt = column[OffsetDateTime]("expires_at")
  def accountUsername = column[String]("account_username")

  override def * = (token, email, expiresAt, accountUsername) <> (TokenInfo.tupled, TokenInfo.unapply _)

}

class TicketTable(tag: Tag) extends Table[Ticket](tag, "ticket") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def reporter = column[String]("reporter")
  def reporterName = column[String]("reporter_name")
  def assignedTo = column[String]("assigned_to")
  def assignedToName = column[String]("assigned_to_name")
  def cc = column[List[String]]("cc")
  def createdAt = column[OffsetDateTime]("created_at")
  def status = column[String]("status")
  def priority = column[String]("priority")
  def title = column[String]("title")
  def description = column[String]("description")
  def resolution = column[String]("resolution")
  def attachment = column[List[String]]("attachment")
  def media = column[String]("media")

  override def * = (
    id.?,
    reporter.?,
    reporterName.?,
    assignedTo.?,
    assignedToName.?,
    cc,
    createdAt,
    status.?,
    priority.?,
    title.?,
    description.?,
    resolution.?,
    attachment,
    media.?
  ) <> ((Ticket.apply _).tupled, Ticket.unapply _)

}

class LabeledTicketTable(tag: Tag) extends Table[LabeledTicket](tag, "labeled_ticket") {

  def description = column[String]("description")
  def assignedTo = column[String]("assigned_to")

  override def * = (description.?, assignedTo.?) <> ((LabeledTicket.apply _).tupled, LabeledTicket.unapply _)

}

class MilestoneTable(tag: Tag) extends Table[Milestone](tag, "milestone") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def ticketId = column[Long]("ticket_id")
  def datetime = column[OffsetDateTime]("datetime")
  def description = column[String]("description")
  def milestoneReporter = column[String]("milestone_reporter")
  def milestoneReporterName = column[String]("milestone_reporter_name")

  override def * = (
    id.?,
    ticketId.?,
    datetime,
    description.?,
    milestoneReporter.?,
    milestoneReporterName.?
  ) <> ((Milestone.apply _).tupled, Milestone.unapply _)

}