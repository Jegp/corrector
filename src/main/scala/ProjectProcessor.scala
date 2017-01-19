import java.nio.file.Path

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

/**
  * Processes projects in a thread.
  */
object ProjectProcessor {

  def apply(root: Path, pmdRoot: Path): Future[ProjectMetric] = {
    evaluate(root, pmdRoot)
  }

  def evaluate(root: Path, pmdRoot: Path): Future[ProjectMetric] = {
    analyseCode(root, pmdRoot).flatMap(codeMetric => {
      testCode(root).map(testMetric => {
        ProjectMetric(codeMetric, testMetric)
      })
    })
  }

  def analyseCode(root: Path, pmdRoot: Path): Future[CodeMetric] = {
    CodeAnalyser.analyse(root, pmdRoot).map {
      case map => CodeMetric(map)
    }
  }

  def testCode(root: Path): Future[TestMetric] = {
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
