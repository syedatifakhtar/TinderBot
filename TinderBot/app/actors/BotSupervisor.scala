package actors

import akka.actor.SupervisorStrategy.{Resume, Restart, Stop, Escalate}
import akka.actor.{OneForOneStrategy, Actor, ActorLogging, Props}
import com.typesafe.config.Config
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

class BotSupervisor(wsClient: WSClient,config: Config) extends Actor with ActorLogging{

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ArithmeticException      => Resume
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception                => Escalate
    }

  val tinderBot =
    context
      .actorOf(Props(new TinderBot(wsClient,config))
        ,"TinderBot")

  override def preStart() = {
    log.info("Starting Tinder Bot Supervisor")
        tinderBot ! TinderBot.Start
  }



  override def receive: Receive = {
    case _ =>
  }
}

object BotSupervisor {
  def props = Props[BotSupervisor]
}
