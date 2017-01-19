import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Utilities for handling maven projects in .zip files.
  */
object ProjectUtils {

  private val mavenTestSource: Array[String] = Array("src", "test", "java")

  /**
    * Extracts a zip file and returns the path to the root of the extracted file tree.
    *
    * @return The root of the extracted zip file.
    */
  def extractZip(file: Path): Either[Exception, Path] = {
    try {
      val targetDir = Files.createTempDirectory("project")
      new ZipFile(file.toFile).extractAll(targetDir.toString)
      normalizeExtractedDirectory(targetDir)
      Right(targetDir)
    } catch {
      case e: Exception => Left(e)
    }
  }

  private def normalizeExtractedDirectory(root: Path): Unit = {
    val list = Files.list(root).collect(Collectors.toList[Path])
    // Examine if the list only contains one directory. If it does, we need to move the directory content to the root
    if (list.size() == 1 && Files.isDirectory(list.get(0))) {
      Files.list(list.get(0)).forEach(path => Files.move(path, root.resolve(path.getFileName)))
      Files.delete(list.get(0)) // Delete empty directory
    }
  }

  /**
    * Copies the test sources from one maven project to another, assuming the files are in src/test/java.
    *
    * @param sourceProject      The source root of the test files.
    * @param destinationProject The destination root of the test file.
    */
  def copyTestFiles(sourceProject: Path, destinationProject: Path): Unit = {
    val sourcePath = Paths.get(sourceProject.toString, mavenTestSource: _*)
    val destinationPath = Paths.get(destinationProject.toString, mavenTestSource: _*)
    // Create directories recursively
    mavenTestSource.foldLeft("")((toCreate, created) => {
      val newPath = if (toCreate.isEmpty) {
        created
      } else {
        toCreate + File.separator + created
      }
      Files.createDirectories(destinationProject.resolve(newPath))
      newPath
    })
    FileUtils.copyDirectory(sourcePath.toFile, destinationPath.toFile)
  }

  /**
    * Extracts the zip file to a directory, copies the given test sources into it and executes a function on the path.
    * This method deletes the project folder after being run.
    *
    * @param zipFile  The zip file from which to extract the files.
    * @param testPath The path to the project containing the test files to include in the current project.
    * @param f        The function to apply on the project.
    * @tparam T The type of the value returned from operation on the project.
    * @return The return value of the function.
    */
  def extractCopyRunAndDelete[T](zipFile: Path, testPath: Path, f: Path => Future[T]): Future[T] = {
    val future = Future {
      extractZip(zipFile).right.map(projectPath => {
        copyTestFiles(testPath, projectPath)
        projectPath
      }) match {
        case Left(exception) => throw exception
        case Right(path) => path
      }
    }

    future.flatMap[T](path => {
      val result = f(path)
      result.onComplete(_ => org.apache.commons.io.FileUtils.deleteDirectory(path.toFile))
      result
    })
  }

}
