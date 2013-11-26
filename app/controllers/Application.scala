package controllers

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import play.api.mvc.{WebSocket, Action, Controller}
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsValue
import play.api.Logger
import play.api.Play.current
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import actors.{GetUser, UsersActor, UserActor}
import play.api.libs.EventSource
import java.util.UUID
import akka.util.Timeout
import scala.concurrent.Future

object Application extends Controller {
  
  def index = Action { implicit request =>
    // the UUID makes it so that a single UserActor can represent a user 
    val uuid = request.session.get("uuid").getOrElse(UUID.randomUUID().toString)
    Ok(views.html.index(uuid)).withSession("uuid" -> uuid)
  }

  // plain ole WebSocket
  def ws(uuid: String) = WebSocket.async[JsValue] { request =>
    Logger.info("User Connected to WebSocket")

    userActor(uuid).map { actorRef =>
      val (out, channel) = Concurrent.broadcast[JsValue]

      // setup the channel for the UserActor
      actorRef ! channel

      val in = Iteratee.foreach[JsValue] { message =>
        Logger.info(s"Received WebSocket message: $message")
        actorRef ! message
      } map { _ =>
        Logger.info("User Disconnected from WebSocket")
        // todo: remove the out channel
      }

      (in, out)
    }
    
  }

  // the push channel that will be coupled with the user for the SSE channel
  def esPush(uuid: String) = Action.async {
    Logger.info("User Connected to SSE Push")
    
    userActor(uuid).map { actorRef =>
      val (out, channel) = Concurrent.broadcast[JsValue]
    
      // setup the channel for the UserActor
      actorRef ! channel
    
      // todo: handle disconnect
      
      Ok.feed(out &> EventSource()).as(EVENT_STREAM)
    }
    
  }
  
  // the receive channel that will be coupled with the user for the SSE channel
  def esPost(uuid: String) = Action.async(parse.json) { request =>
    userActor(uuid).map { actorRef =>

      // send the JsValue on to the UserActor
      actorRef ! request.body

      Ok
    }
  }
  
  private def userActor(uuid: String): Future[ActorRef] = {
    implicit val timeout = Timeout(1.second)
    (UsersActor.usersActor ? GetUser(uuid)).mapTo[ActorRef]
  }

}