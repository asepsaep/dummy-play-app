package forms

import play.api.data.Form
import play.api.data.Forms._

object ForgotPassword {
  val form = Form(
    mapping(
      "email" → email
    )(Data.apply)(Data.unapply)
  )

  /**
   * The form data.
   *
   * @param email email
   */
  case class Data(
    email: String
  )

}
