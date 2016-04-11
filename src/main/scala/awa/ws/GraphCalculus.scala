package awa.ws

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import awa.graph._

import scala.collection.immutable.ListMap
import scala.util.matching.Regex
import scala.concurrent.duration._


trait GraphCalculus {
  def calculusFlow(sender: String): Flow[String, String, Any]

  def injectMessage(message: String): Unit
}


object GraphCalculus {
  def create(system: ActorSystem): GraphCalculus = {
    // The implementation uses a single actor to collect and distribute
    // messages. It would be nicer if this could be built by stream operations
    // directly.
    val masterActor =
      system.actorOf(Props(new Actor {
        import context._

        var subscribers = Set.empty[(String, ActorRef)]

        // timeout websockets
        context.system.scheduler.schedule(0 milliseconds, 50 seconds) {
          dispatch("")
        }

        def receive: Receive = {
          // a new participant joins the calculus
          case NewParticipant(name, subscriber) ⇒
            context.watch(subscriber)
            subscribers += (name -> subscriber)
            //dispatch( "Joined: " + name + "-" +members)

          // a simple message was received. Send to the others in the room
          case msg: ReceivedMessage => dispatch(msg.toChatMessage)

          // send a simple message to others
          case msg: String => dispatch(msg)

          // somebody legft the room
          case ParticipantLeft(person) =>
            val entry @ (name, ref) = subscribers.find(_._1 == person).get
            // report downstream of completion, otherwise, there's a risk of leaking the
            // downstream when the TCP connection is only half-closed
            ref ! Status.Success(Unit)
            subscribers -= entry
            dispatch( "Left:" + person + "-" + members)

          // terminate participants
          case Terminated(sub) =>
            // clean up dead subscribers, but should have been removed when `ParticipantLeft`
            subscribers = subscribers.filterNot(_._2 == sub)

          // commands received.
          case ReceivedCommand(command, text) =>
            command match {
              case "#start#" => {
                // create the graph and read the info
                val g = new Graph()
                if (g.createGraphFromText(text)) {
                  var result = Map[Int, Float]()

                  // calculate the shortest paths for each node
                  for (n <- g.nodes) {
                    val sh = g.findShortest(n._1)
                    result += (n._1 -> sh)
                  }

                  // sort the nodes
                  val sortedShortest = ListMap(result.toSeq.sortBy(-_._2):_*)
                  dispatch(s"RESULT: ${sortedShortest.head}")
                } else {
                  dispatch("Error creating graph. Please check.")
                }
              }
              case "#stop#" => dispatch("STOPED CALCULUS:" + command + " - " + text)
              case _ => dispatch("UNK CALCULUS:" + command + " - " + text)

          }

        }
        def dispatch(msg: String): Unit = subscribers.foreach(_._2 ! msg)
        def members = subscribers.map(_._1).toSeq
      }))

    // Wraps the chatActor in a sink. When the stream to this sink will be completed
    // it sends the `ParticipantLeft` message to the chatActor.
    // FIXME: here some rate-limiting should be applied to prevent single users flooding the chat
    def chatInSink(sender: String) = Sink.actorRef[ChatEvent](masterActor, ParticipantLeft(sender))

    new GraphCalculus {
      def calculusFlow(sender: String): Flow[String, String, Any] = {
        val in =
          Flow[String]
            .map(checkCommand(sender, _))
            .to(chatInSink(sender))

        // The counter-part which is a source that will create a target ActorRef per
        // materialization where the chatActor will send its messages to.
        // This source will only buffer one element and will fail if the client doesn't read
        // messages fast enough.
        val out =
          Source.actorRef[String](1, OverflowStrategy.fail)
            .mapMaterializedValue(masterActor ! NewParticipant(sender, _))

        Flow.fromSinkAndSource(in, out)
      }
      def injectMessage(message: String): Unit = masterActor ! message // non-streams interface

      // a command will come between # and #
      def checkCommand(sender: String, msg: String): ChatEvent ={
        val pattern = new Regex("^#(\\w+)#")
        // use a regex to check for a command
        val command = pattern.findFirstIn(msg)
        if (!command.isEmpty) {
          // dispatch a command message to the  actor
          val text = msg.drop(command.get.size)
          ReceivedCommand(command.get, text)
        } else {
          // dispatch a regular command
          ReceivedMessage(sender, msg)
        }
      }
    }
  }

  private sealed trait ChatEvent
  private case class NewParticipant(name: String, subscriber: ActorRef) extends ChatEvent
  private case class ParticipantLeft(name: String) extends ChatEvent
  private case class ReceivedMessage(sender: String, message: String) extends ChatEvent {
    def toChatMessage: String = sender + message
  }
  private case class ReceivedCommand(command: String, text: String) extends ChatEvent
}