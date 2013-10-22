package actors

import akka.actor.Actor
import play.api.libs.json.JsValue
import play.api.libs.iteratee.Concurrent.Channel
import play.api.Logger

class UserActor(channel: Channel[JsValue]) extends Actor {
  
  def receive = {
    case message: JsValue =>
      Logger.info(s"Sending Message: $message")
      channel.push(message)
  }
  

}
