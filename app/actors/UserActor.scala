package actors

import akka.actor.{Props, Actor}
import play.api.libs.json.JsValue
import play.api.libs.iteratee.Concurrent.Channel
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.Play.current 

class UserActor extends Actor {
  
  var maybeChannel: Option[Channel[JsValue]] = None
  
  def receive = {
    case channel: Channel[JsValue] =>
      Logger.info("Setting up channel")
      maybeChannel = Some(channel)
    
    case message: JsValue =>
      if (maybeChannel.isDefined) {
        Logger.info(s"Sending Message: $message")
        maybeChannel.get.push(message)
      }
      else {
        Logger.error("Tried to push a message without a channel")
      }
  }

}

class UsersActor extends Actor {

  def receive = {
    case GetUser(uuid) =>
      // get the child actor with the given uuid or create a new one
      // then reply with that ActorRef
      sender ! context.child(uuid).getOrElse(context.actorOf(Props(classOf[UserActor]), uuid))
  }
  
}

object UsersActor {
  lazy val usersActor = Akka.system.actorOf(Props[UsersActor])
}

case class GetUser(uuid: String)