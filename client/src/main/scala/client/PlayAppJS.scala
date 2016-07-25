package client

import org.scalajs.dom
import org.scalajs.dom._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
//import org.scalajs.jquery.jQuery

object PlayAppJS extends js.JSApp {

  val d = dom.document
  var ws: WebSocket = _

  val buildInfoField = d.getElementById("buildinfo")

  def main(): Unit = {
    ws = new dom.WebSocket("ws://127.0.0.1:9000/ws")
    ws.onopen = (x: Event) ⇒ Console.println("Connecting to WebSocket... " + x.toString)
    ws.onerror = (x: ErrorEvent) ⇒ Console.println("some error has occured... " + x.message)
    ws.onmessage = (x: MessageEvent) ⇒ appendBuildInfo(x.data.toString)
    ws.onclose = (x: CloseEvent) ⇒ Console.println("Closing WebSocket connection... " + x.toString)
  }

  @JSExport
  def logout(): Unit = {
    // jQuery("#logout").asInstanceOf[html.Form].submit()
    d.getElementById("logout").asInstanceOf[html.Form].submit()
  }

  @JSExport
  def buildModel() = {
    ws.send("buildModel")
  }

  //  @JSExport
  //  def initWS() = {
  //    Console.println("Init WebSocket button clicked")
  //  }

  def appendBuildInfo(newInfo: String) = {
    val p = d.createElement("p")
    p.innerHTML = s"$newInfo<br>"
    buildInfoField.appendChild(p)
  }

}
