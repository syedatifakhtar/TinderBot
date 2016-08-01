package controllers

import javax.inject._

import actors.BotSupervisor
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.Configuration

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (system: ActorSystem,wSClient: WSClient) extends Controller {

  val configs = ConfigFactory.load
  val botSupervisor = system.actorOf(Props(new BotSupervisor(wSClient,configs)),"TinderBotSupervisor")

  def index = Action {
    Ok("TinderBot up and running")
  }

}
