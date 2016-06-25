package client

import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
//import org.scalajs.jquery.jQuery

object PlayAppJS extends js.JSApp {

  def main(): Unit = {

  }

  @JSExport
  def logout(): Unit = {
    // With jQuery
    // jQuery("#logout").asInstanceOf[html.Form].submit()

    dom.document.getElementById("logout").asInstanceOf[html.Form].submit()
  }

}
