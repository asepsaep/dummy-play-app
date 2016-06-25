package utils

import javax.inject.Inject

import models.{ Account, TokenInfo }
import models.daos.TokenInfoDAO
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.mailer._

class EmailSender @Inject() (
  mailerClient: MailerClient,
  val messagesApi: MessagesApi
) extends I18nSupport {

  def emailConfirmation(tokenInfo: TokenInfo, account: Account) = {
    val recipient = account.name.fold(tokenInfo.email)(name ⇒ name + " <" + tokenInfo.email + ">")
    val link = s"${Messages("site")}/verify?token=${tokenInfo.token}&email=${tokenInfo.email}"
    val email = Email(
      subject = Messages("email.confirm.title"),
      from = Messages("email.from"),
      to = Seq(recipient),
      bodyHtml =
        Some(
          s"""
           |<html></body><br>
           |${Messages("email.confirm.link")}<br>
           |<br>
           |<a href="$link">${Messages("email.confirm.title")}</a><br>
           |<br>
           |${Messages("email.confirm.altlink")}<br>
           |<br>
           |$link<br>
           |<br>
           |${Messages("email.confirm.disclaimer")}
             |</body></html>
         """.stripMargin
        )
    )
    mailerClient.send(email)
  }

  def resetPassword(tokenInfo: TokenInfo, account: Account) = {
    val recipient = account.name.fold(tokenInfo.email)(name ⇒ name + " <" + tokenInfo.email + ">")
    val link = s"${Messages("site")}/reset-password?token=${tokenInfo.token}&email=${tokenInfo.email}"
    val email = Email(
      subject = Messages("reset.password.title"),
      from = Messages("email.from"),
      to = Seq(recipient),
      bodyHtml =
        Some(
          s"""
           |<html></body><br>
           |${Messages("reset.password.message")}<br>
           |${Messages("reset.password.link")}<br>
           |<br>
           |<a href="$link">${Messages("reset.password.title")}</a><br>
           |<br>
           |${Messages("reset.password.altlink")}<br>
           |<br>
           |$link<br>
           |<br>
           |${Messages("reset.password.addinfo")}<br>
           |<br>
           |${Messages("reset.password.disclaimer")}<br>
           |</body></html>
         """.stripMargin
        )
    )
    mailerClient.send(email)
  }

}