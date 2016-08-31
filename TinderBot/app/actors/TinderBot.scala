package actors

import java.io.PrintWriter

import actors.TinderBot._
import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.Config
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient
import scala.concurrent.duration._


class TinderBot(wSClient: WSClient, configs: Config) extends Actor with ActorLogging{

  import net.ceedubs.ficus.Ficus._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  def writeToFile(filePath: String,contents: String): Unit ={
      new PrintWriter(filePath){
        write(contents)
        close
      }
  }



  def parseRecResponse(recs: JsValue)(authHeader: Array[(String,String)]) = {
    debug("JSON")
    val users = recs \\ "user"
    users.foreach {
      user=>
        debug(s"Liking ${(user \ "name").as[String]  }")
        val likeUrl = s"https://api.gotinder.com/like/${(user \ "_id").as[String]}?"+
          s"${((user \ "photos").as[List[JsObject]].head \ "id").as[String]}&content_hash=${(user \ "content_hash").as[String]}"

        debug(s"Like Url: ${likeUrl}")
        wSClient
          .url(likeUrl)
          .withHeaders(authHeader: _*)
          .get
            .map{
                response=>
                 if(response.status==200)
                 debug("Like successfull")
                 debug(response.body)
              }

    }

  }


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
  val userDataFilePath = configs.as[String]("tinderBot.userDataFilePath")
  val userDataFileName = configs.as[String]("tinderBot.userDataFileName")
  val matchDataFilePath = configs.as[String]("tinderBot.matchDataFilePath")
  val matchDataFileName = configs.as[String]("tinderBot.matchDataFileName")

  case class authData(facebook_id: String,facebook_token: String)

  object authData{
    implicit val authDataReads = Json.writes[authData]
  }

  val jsonData = Json.toJson(authData(userFacebookId,userFacebookToken))

  def debug = {message: String => println(s"\n\n----------${message}---------------\n")}
  def debugWithArgs
    (header: String)(args: (String,String)*) = {
        log.debug(s"\n\n----------${header}---------------\n")
        args.foreach{
          arg=>
          log.debug(s"\n${arg._1} : ${arg._2}")
        }
  }

  def initialized: Receive = {
    case Start =>
      debug("Firing Auth Request")
      debugWithArgs(s"Json Data")("Data"->jsonData.toString)
      val thisActor = self
      wSClient
        .url("https://api.gotinder.com/auth")
        .withHeaders(tinderAuthHeaders: _*)
        .post(jsonData)
          .map{
            response=>
              writeToFile(s"${userDataFilePath}/${System.currentTimeMillis}-${userDataFileName}",response.body)
              val token = (response.json \ "token").get
              debug("Becoming ready!")
              context become ready
              self ! Poll(token.as[String])
          }
  }

  def ready: Receive = {
    case Poll(token: String) =>
      debug("Starting to poll for match data files--->")
      debug(s"Token: ${token}")
      val authWithToken = tinderAuthHeaders.+:("X-Auth-Token"->token)
      wSClient
        .url("https://api.gotinder.com/user/recs?locale=en")
        .withHeaders(authWithToken: _*)
        .get
          .map{
            response=>
              writeToFile(s"${matchDataFilePath}/${System.currentTimeMillis}-${matchDataFileName}",response.body)
              parseRecResponse(response.json)(authWithToken)
          }
      context.system.scheduler.scheduleOnce(1 minute,self,Poll(token))

  }
}

object TinderBot {

  case object Start
  case class Poll(token: String)
  case object NoTokenFoundException extends Exception("Did not receive any auth token fro the auth request.Failing....")
}
