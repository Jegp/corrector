import java.nio.file.Path
import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

/**
  * Compiles and loads a project into the classpath.
  */
object ProjectTester {

  private val testPrefix = "Results :"
  private val testPostfix = "------------------------------------------------------------------------"

  val policyFile = ProjectTester.getClass.getResource("sandbox.policy").toString

  private val sandboxArgument = {
    s"""-DargLine="-Djava.security.manager -Djava.security.policy==$policyFile""""
  }

  def compileAndTest(root: Path): Future[String] = {
    val p = new ProcessBuilder()

    p.directory(root.toFile)
    p.environment().put("java.security.policy", policyFile)
    p.command("mvn", sandboxArgument, "test")

    val process = p.start()
    val output = process.getInputStream

    Future {
      val out = Source.fromInputStream(output)
      if (process.waitFor(5, TimeUnit.MINUTES)) {
        out.getLines()
          .dropWhile(!_.contains(testPrefix)) // Drop prefix
          .takeWhile(!_.contains(testPostfix)) // Drop postfix
          .mkString("\n")
      } else {
        process.destroyForcibly()
        throw new RuntimeException("Error waiting for process. Shutting down")
      }
    }
  }

}
