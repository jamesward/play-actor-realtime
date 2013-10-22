package controllers

import akka.actor.Props
import play.api.mvc.{WebSocket, Action, Controller}
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsValue
import play.api.Logger
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import actors.UserActor

object Application extends Controller {
  
  def index = Action { request =>
    Ok(views.html.index.render(request))
  }

  def ws = WebSocket.using[JsValue] { request =>
    Logger.info("User Connected")

    val (out, channel) = Concurrent.broadcast[JsValue]

    val userActor = Akka.system.actorOf(Props(classOf[UserActor], channel))
    
    val in = Iteratee.foreach[JsValue] { message =>
      Logger.info(s"Received message: $message")
      userActor ! message
    }.map { _ =>
      Logger.info("User Disconnected")
      Akka.system.stop(userActor)
    }
    
    (in, out)
  }

}