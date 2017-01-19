/**
  * Measurements of code quality and test reports from a Java project.
  *
  * @param code The metrics for the code quality.
  * @param test The metric for test results.
  */
case class ProjectMetric(code: CodeMetric, test: TestMetric)

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
