import java.io.FileNotFoundException
import java.nio.file.{Files, Paths}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Command-line entry point
  */
object Main {

  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      throw new IllegalArgumentException(
        "Needs location of .jar file, location of project containing test files and pmd directory as input")
    }

    val jarFile = Paths.get(args(0))
    if (!Files.exists(jarFile)) {
      throw new FileNotFoundException(jarFile.toString)
    }

    val sourceRoot = Paths.get(args(1))
    if (!Files.isDirectory(sourceRoot)) {
      throw new FileNotFoundException(sourceRoot.toString)
    }

    val pmdDir = Paths.get(args(2))
    if (!Files.isDirectory(pmdDir)) {
      throw new FileNotFoundException(pmdDir.toString)
    }

    val metricFuture = ProjectUtils.extractCopyRunAndDelete(jarFile, sourceRoot, path => ProjectProcessor(path, pmdDir))
    println("Analysing code, please wait...")
    val future = metricFuture.map(printMetrics)
    Await.result(future, 4 minutes)
  }

  private def printMetrics(metrics: ProjectMetric): Unit = {
    println(
      s"""
        <html>
        <head>
        <title>Project analyser</title>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/materialize/0.97.8/css/materialize.min.css">
          <script src="https://cdnjs.cloudflare.com/ajax/libs/materialize/0.97.8/js/materialize.min.js"></script>
        </head>
        <body>
        <div class="row">
        <div class="col s6">
        <h1>JUnit tests</h1>
        <pre>${metrics.test.testResults}</pre>
        </div>
        <div class="col s6">
        ${metrics.code.codeReport}
        </div>
        </div>
        </body>
        </html>
    """.stripMargin)
  }

}
