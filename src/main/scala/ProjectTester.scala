import java.nio.file.{Path, Paths}
import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
  * Compiles and tests a project using Scala Build Tool sbt.
  *
  * The project tester assumes the sbt command is in classpath.
  */
object ProjectTester {

  private val testPrefixRegex = "\\[info\\] [!\\+>]".r

  val policyFile = ProjectTester.getClass.getResource("sandbox.policy").toString

  def compileAndTest(root: Path): Future[String] = {
    val sbtBuilder = new ProcessBuilder()

    sbtBuilder.directory(root.toFile)
    sbtBuilder.command("sbt", "test")

    val sbt = sbtBuilder.start()
    val output = sbt.getInputStream

    Future {
      if (sbt.waitFor(5, TimeUnit.MINUTES)) {
        val out = Source.fromInputStream(output)
        out.getLines().toSeq
          .map(line => AnsiCodeParser.ansiRegex.replaceAllIn(line, "")) // Remove ansi codes
          .dropWhile(line => testPrefixRegex.findFirstIn(line).isEmpty) // Drop prefix
          .mkString("\n")
      } else {
        sbt.destroyForcibly()
        throw new RuntimeException("Error waiting for process. Shutting down")
      }
    }
  }

}
