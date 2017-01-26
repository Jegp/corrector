/**
  * Measurements of code quality and test reports from a Java project.
  *
  * @param code The metrics for the code quality.
  * @param test The metric for test results.
  */
case class ProjectMetric(code: CodeMetric, test: TestMetric) {

  def toHtml: String = {
    s"""<html>
        <head>
        <title>Project analyser</title>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/materialize/0.97.8/css/materialize.min.css">
          <script src="https://cdnjs.cloudflare.com/ajax/libs/materialize/0.97.8/js/materialize.min.js"></script>
        </head>
        <body>
        <div class="row">
        <div class="col s6">
        <h1>Property tests</h1>
        <pre>${test.testResults}</pre>
        </div>
        <div class="col s6">
        <h1>Code analysis</h1>
        ${code.codeReport}
        </div>
        </div>
        </body>
        </html>
    """.stripMargin
  }

}

/**
  * Metrics for code quality.
  *
  * @param codeReport The report from a static code analysis.
  */
case class CodeMetric(codeReport: String)

/**
  * The test results for a test suite.
  *
  * @param testResults For each test, either a stack trace of what went wrong or nothing (success).
  */
case class TestMetric(testResults: String)
