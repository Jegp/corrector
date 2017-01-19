import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.{Source => IoSource}

/**
  * Created by jens on 15.01.17.
  */
object WebServer {

  private def indexHtml = IoSource.fromInputStream(WebServer.getClass.getResourceAsStream("index.html")).mkString

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      throw new IllegalArgumentException("Need two arguments: host and port")
    }

    run(args(0), Integer.decode(args(1)))
  }

  def run(host: String, port: Int): Unit = {
    implicit val system = ActorSystem("hugin")
    // needed for the future flatMap/onComplete in the end
    implicit val materializer = ActorMaterializer()

    // Websocket request
    val websockeRoute = Flow[Message].flatMapConcat {
      case bm: BinaryMessage => processProject(bm)
      case e: Any => Source.single(TextMessage("Unknown message " + e))
    }

    val route =
      path("") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, indexHtml))
        }
      } ~
        path("assignment1") {
          handleWebSocketMessages(websockeRoute)
        }

    val binding = Http().bindAndHandle(route, host, port)

    // Wait for user to press enter
    scala.io.StdIn.readLine()
    binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  private def processProject(message: BinaryMessage): Source[Message, NotUsed] = {
    Source.empty
  }

}
