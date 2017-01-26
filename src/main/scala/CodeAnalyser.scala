import java.nio.file.Path
import java.util.concurrent.{TimeUnit, TimeoutException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
  * Analyses a project using a static code analyser.
  */
object CodeAnalyser {

  // @formatter:off
  val rulesets = Seq(
    "basic",
    "braces",
    "clone",
    "codesize",
    "comments",
    // "controversial",
    "design",
    "empty",
    // "finalizers",
    "imports",
    "junit",
    "optimizations",
    "strictexception",
    "strings",
    "typeresolution",
    "unnecessary",
    "unusedcode"
  )
  // @formatter:on

  val pmdWebsite = "https://pmd.github.io/pmd-5.5.2/pmd-java"
  val pmdWebsiteRegex = """\$\{pmd.website.baseurl\}""".r

  /**
    * Analyse a project by running the 'pmd' tool on it.
    *
    * @param root    The root of the project to analyse.
    * @param pmdRoot The root path of the pmd tool, including a ruleset subdirectory with the rule .xml files.
    * @return A document in html.
    */
  def analyse(root: Path, pmdRoot: Path): Future[String] = {
    Future {
      val builder = new ProcessBuilder()
      builder.directory(root.toFile)
      // Set pmd website
      val command = Seq(pmdRoot.resolve("bin").resolve("run.sh").toString, "pmd",
        "-d", ".",
        "-language", "java",
        "-version", "1.8",
        "-f", "html",
        "-rulesets", rulesets.map(rule => s"${pmdRoot.toString}/rulesets/java/$rule.xml").mkString(","))
      builder.command(command: _*)

      val process = builder.start()
      val output = process.getInputStream
      val outputString = Future {
        val out = Source.fromInputStream(output)
          // Remove unnecessary project tmp path
          .getLines().map(line => line.replace(root.toString, ""))
          // Make one long string
          .mkString

        // Strip away surrounding <html>, <head> and <h*> tags
        val stripped = out.substring(119, out.length - 15)

        // Seriously, it's easier to parse the links than read the documentation on how to set the pmd website -_-
        pmdWebsiteRegex.replaceAllIn(stripped, pmdWebsite)
      }

      if (process.waitFor(2, TimeUnit.MINUTES)) {
        Await.result(outputString, 10 seconds)
      } else {
        process.destroyForcibly()
        throw new TimeoutException("Code analysis did not complete within 2 minutes. Exit code " + process.exitValue())
      }
    }
  }

}
