package actors

import actors.TinderBot._
import akka.actor.Actor
import com.typesafe.config.Config
import play.api.{Configuration, Play}
import play.api.libs.ws.WSClient



class TinderBot(wSClient: WSClient, configs: Config) extends Actor{

  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import net.ceedubs.ficus.Ficus._

  val tinderConfigs = Map(

  )

  override def receive: Receive = initialized


  val tinderAuthHeaders = Map(
    "User-Agent"      -> configs.as[String]("tinderBot.userAgent"),
    "app-version"     -> configs.as[String]("tinderBot.appVersion"),
    "os-version"      -> "23",
    "Facebook-ID"     -> configs.as[String]("tinderBot.userFacebookId"),
    "Accept-Language" -> "en",
    "Content-Type"    -> "application/json; charset=utf-8",
    "Connection"      -> "Keep-Alive",
    "Accept-Encoding" -> "gzip"
  ).toArray

  val jsonData = s"{\"facebook_id\": \"${configs.as[String]("tinderBot.userFacebookId")}\"" +
    s",\"facebook_token\": \"${configs.as[String]("tinderBot.userFacebookToken")}\"}"


  def initialized: Receive = {
    case Start =>
      println("Firing AUTH request")
      wSClient
        .url("https://api.gotinder.com/auth")
        .withHeaders(tinderAuthHeaders: _*)
        .post(jsonData)
          .map{
            response=>
            println(response.body)
      }
  }
}

object TinderBot {

  case object Start
}
