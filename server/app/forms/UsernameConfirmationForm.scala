package forms

import play.api.data.Form
import play.api.data.Forms._

object UsernameConfirmationForm {

  val form = Form(
    mapping(
      "username" → nonEmptyText(minLength = 4, maxLength = 30)
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    username: String
  )

}
