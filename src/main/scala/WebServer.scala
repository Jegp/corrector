import java.nio.file.{Files, Path, Paths}

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, RejectionHandler}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.{Source => IoSource}
import scala.util.{Failure, Success}

/**
  * Starts a webserver that can push to a service
  */
object WebServer {

  private def indexHtml = IoSource.fromInputStream(WebServer.getClass.getResourceAsStream("index.html")).mkString

  def main(args: Array[String]): Unit = {
    if (args.length != 4) {
      throw new IllegalArgumentException("Need four arguments: host, port, path to test sbt projects and path to pmd")
    }

    run(args(0), Integer.decode(args(1)), Paths.get(args(2)), Paths.get(args(3)))
  }

  def run(host: String, port: Int, sourceRoot: Path, pmdRoot: Path): Unit = {
    implicit val system = ActorSystem("hugin")
    // needed for the future flatMap/onComplete in the end
    implicit val materializer = ActorMaterializer()

    val fileCache = FileCache()

    val route =
      path("") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, indexHtml))
        }
      } ~ path(Remaining) { userId =>
        get {
          onComplete(fileCache.get(userId)) {
            case Success(html) => complete(HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, html)))
            case Failure(error) => complete(HttpResponse(StatusCodes.NotFound))
          }
        }
      } ~ path(Segment / Remaining) { (assignment: String, userId: String) =>
        put {
          extractDataBytes { bytes =>
            val tmpFile = Files.createTempFile("zip", null)
            val writeFuture = bytes.runWith(FileIO.toPath(tmpFile))

            // Let the user know it's being processed
            fileCache.put(userId, s"Processing exercise $assignment... " +
              "Go <a href=\"https://en.wikipedia.org/wiki/Special:Random\">read something random</a> while you wait")

            // Let this run asynchronously
            writeFuture.flatMap(result => {
              result.status match {
                case Success(Done) => ProjectProcessor(tmpFile, sourceRoot.resolve(assignment), pmdRoot)
                case Failure(error) => Future.failed(error)
              }
            }).onComplete({
              case Success(metric) => fileCache.put(userId, metric.toHtml)
              case Failure(error) => fileCache.put(userId, error.toString)
            })

            onComplete(writeFuture) {
              case Success(metric) =>
                complete(StatusCodes.OK)
              case Failure(error) =>
                System.err.println(error)
                complete(HttpResponse(StatusCodes.BadRequest, entity = error.toString))
            }
          }
        }
      }

    val binding = Http().bindAndHandle(route, host, port)

    // Wait for user to press enter
    scala.io.StdIn.readLine()
    fileCache.erase()
    binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

}
