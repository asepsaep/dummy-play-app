package forms

import play.api.data.Form
import play.api.data.Forms._

object RegisterForm {

  val form = Form(
    mapping(
      "username" → nonEmptyText(minLength = 4, maxLength = 30),
      "name" → nonEmptyText,
      "email" → email,
      "password" → nonEmptyText(minLength = 6)
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    username: String,
    name: String,
    email: String,
    password: String
  )
}
