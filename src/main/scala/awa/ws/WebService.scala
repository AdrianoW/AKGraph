package awa.ws

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.stage._

import scala.concurrent.duration._

import akka.http.scaladsl.server.Directives
import akka.stream.Materializer
import akka.stream.scaladsl.Flow


class WebService(implicit fm: Materializer, system: ActorSystem) extends Directives {
  val calculator = GraphCalculus.create(system)

  def route =
    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        // Scala-JS puts them in the root of the resource directory per default,
        // so that's where we pick them up
        //path("frontend-launcher.js")(getFromResource("frontend-launcher.js")) ~
        //path("frontend-fastopt.js")(getFromResource("frontend-fastopt.js")) ~
        path("calculate") {
          parameter('id) { id =>
            handleWebSocketMessages(websocketChatFlow(sender = id))
          }
        } ~
        path("graph") {
          getFromResource("web/ws.html")
        }
    } ~
      getFromResourceDirectory("web")

  def websocketChatFlow(sender: String): Flow[Message, Message, Any] =
    Flow[Message]
      .collect {
        case TextMessage.Strict(msg) ⇒ msg // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }
      .via(calculator.calculusFlow(sender)) // ... and route them through the chatFlow ...
      .map {
      case msg: String =>
        TextMessage.Strict(msg) // ... pack outgoing messages into WS JSON messages ...
    }
      .via(reportErrorsFlow) // ... then log any processing errors on stdin

  def reportErrorsFlow[T]: Flow[T, T, Any] =
    Flow[T]
      .transform(() ⇒ new PushStage[T, T] {
        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)

        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
          println(s"WS stream failed with $cause")
          super.onUpstreamFailure(cause, ctx)
        }
      })
}
