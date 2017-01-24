import java.io.FileNotFoundException
import java.nio.file.{Files, Path, Paths}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Command-line entry point.
  */
object Main {

  val pmdRoot: Path = Paths.get(".").resolve("pmd").toAbsolutePath

  /**
    * Takes a maven project from and 1) performs a static code analysis on the code inside the project and 2) tests
    * the code in the project against test-files in another project.
    *
    * @param args Requires the first entry to be a location of a maven project and the second entry to be the path
    *             to a maven project which contains test files to be tested against the project inside zipped file.
    */
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      throw new IllegalArgumentException(
        "Needs two inputs: the location of a maven project and the location of a maven project containing " +
          "test files. Please see documentation.")
    }

    val input = Paths.get(args(0))
    if (!Files.exists(input)) {
      throw new FileNotFoundException(input.toString)
    }

    val sourceRoot = Paths.get(args(1))
    if (!Files.isDirectory(sourceRoot)) {
      throw new FileNotFoundException(sourceRoot.toString)
    }

    val pmdRoot = Paths.get(".").resolve("pmd")

    println(Await.result(ProjectProcessor(input, sourceRoot, pmdRoot), 4 minutes).toHtml)
  }

}
