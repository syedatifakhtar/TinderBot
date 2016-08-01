package actors

import actors.TinderBot._
import akka.actor.Actor
import com.typesafe.config.Config
import play.api.libs.json.Json
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
    "platform"        -> configs.as[String]("tinderBot.platform"),
    "app-version"     -> configs.as[String]("tinderBot.appVersion"),
    "os-version"      -> configs.as[String]("tinderBot.osVersion"),
    "Facebook-ID"     -> configs.as[String]("tinderBot.userFacebookId"),
    "host"            -> configs.as[String]("tinderBot.authHost"),
    "Accept-Language" -> "en",
    "Content-Type"    -> "application/json; charset=utf-8",
    "Connection"      -> "Keep-Alive",
    "Accept-Encoding" -> "gzip"
  ).toArray

  val userFacebookId    = configs.as[String]("tinderBot.userFacebookId")
  val userFacebookToken = configs.as[String]("tinderBot.userFacebookToken")

  case class authData(facebook_id: String,facebook_token: String)

  object authData{
    implicit val authDataReads = Json.writes[authData]
  }

  val jsonData = Json.toJson(authData(userFacebookId,userFacebookToken))


  def initialized: Receive = {
    case Start =>
      println("Firing AUTH request")
      tinderAuthHeaders.map(x=>println(x.toString))

      println(s"\n\n\n----------Json Data---------------\n${jsonData}")
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
