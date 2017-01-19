import java.io.IOException
import java.nio.file.{Files, Path}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Random, Success}

/**
  * Processes projects by analysing them with a static code analyser and testing the source code. The
  * tests are run asynchronously.
  */
object ProjectProcessor {

  /**
    * Analyses the code in the given path with the PMD tool from the given directory.
    *
    * @param root    The root directory of the maven project to analyse.
    * @param pmdRoot The root directory of the PMD tool.
    * @return Some [[CodeMetric]] in the future.
    */
  def analyseCode(root: Path, pmdRoot: Path): Future[CodeMetric] = CodeAnalyser.analyse(root, pmdRoot).map(CodeMetric)

  /**
    * Analyses the input (be it a .zip (or .jar) file or a maven project directory root by employing a
    * static code analyser (PMD) and testing the sources against source codes from another maven project directory
    * (``sourceRoot``). The analysis returns a [[ProjectMetric]] sometime in the future.
    *
    * @param input      The input. A .zip (or .jar/.war) file or maven project directory root is expected
    * @param sourceRoot The root of the maven project containing the source codes to test against this project.
    * @param pmdRoot    The root of the PMD static code analysing tool.
    * @return A [[ProjectMetric]] sometime in the future.
    */
  def apply(input: Path, sourceRoot: Path, pmdRoot: Path): Future[ProjectMetric] = {
    if (ProjectUtils.isZip(input)) {
      ProjectUtils.extractZip(input) match {
        case Failure(exception) => Future.failed(new IOException("Failed to unzip project", exception))
        case Success(path) => copyAndEvaluate(path, sourceRoot, pmdRoot)
      }
    } else if (Files.isDirectory(input)) {
      copyAndEvaluate(input, sourceRoot, pmdRoot)
    } else {
      Future.failed(new IllegalArgumentException(s"Path '$input' is not .zip file or directory "))
    }
  }

  private def copyAndEvaluate(root: Path, sourceRoot: Path, pmdRoot: Path): Future[ProjectMetric] = {
    ProjectUtils.copyTestFiles(sourceRoot, root)
    evaluate(root, pmdRoot).andThen {
      case t =>
        org.apache.commons.io.FileUtils.deleteDirectory(root.toFile)
        t
    }
  }

  private def evaluate(root: Path, pmdRoot: Path): Future[ProjectMetric] = {
    analyseCode(root, pmdRoot).flatMap(codeMetric => {
      testCode(root).map(testMetric => {
        ProjectMetric(codeMetric, testMetric)
      })
    })
  }

  private def testCode(root: Path): Future[TestMetric] = {
    ProjectTester.compileAndTest(root).map { output =>
      TestMetric(output)
    }
  }

}

sealed case class ProjectId(id: String)

object ProjectId {
  def apply(): ProjectId = {
    ProjectId(new Random().alphanumeric.take(4).mkString)
  }
}
