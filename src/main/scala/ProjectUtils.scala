import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}

/**
  * File-utilities for e. g. handling maven projects and copying files to/from the maven project structure.
  */
object ProjectUtils {

  private val sbtBuildFile = "build.sbt"
  private val sbtTestSource: Array[String] = Array("src", "test", "scala")

  /**
    * Copies the test sources and build.sbt from one sbt project to another,
    * assuming the files are in src/test/scala.
    *
    * @param sourceProject      The source root of the test files.
    * @param destinationProject The destination root of the test file.
    */
  def copyTestFiles(sourceProject: Path, destinationProject: Path): Unit = {
    val sourcePath = Paths.get(sourceProject.toString, sbtTestSource: _*)
    val destinationPath = Paths.get(destinationProject.toString, sbtTestSource: _*)
    // Create directories recursively
    sbtTestSource.foldLeft("")((toCreate, created) => {
      val newPath = if (toCreate.isEmpty) {
        created
      } else {
        toCreate + File.separator + created
      }
      Files.createDirectories(destinationProject.resolve(newPath))
      newPath
    })
    FileUtils.copyDirectory(sourcePath.toFile, destinationPath.toFile)

    // Copy build.sbt
    FileUtils.copyFile(sourceProject.resolve(sbtBuildFile).toFile, destinationProject.resolve(sbtBuildFile).toFile)
  }

  /**
    * Extracts a zip file and returns the path to the root of the extracted file tree.
    *
    * @return The root of the extracted zip file.
    */
  def extractZip(file: Path): Try[Path] = {
    try {
      val targetDir = Files.createTempDirectory("project")
      new ZipFile(file.toFile).extractAll(targetDir.toString)
      normalizeExtractedDirectory(targetDir)
      Success(targetDir)
    } catch {
      case e: Exception => Failure(e)
    }
  }

  /**
    * Determines whether the given input is a valid zip file.
    *
    * @param input The input path of a file or directory.
    * @return True if the file is a valid zip file.
    */
  def isZip(input: Path): Boolean = {
    new ZipFile(input.toFile).isValidZipFile
  }

  /**
    * Makes sure the extracted directory contains all the necessary files in the immediate directory. For instance
    * an extracted jar file with its contents in /tmp/project/maven-project-name-1.0-SNAPSHOT/ will be 'extracted' to
    * /tmp/project/
    *
    * @param root The path of the root of the maven project.
    */
  private def normalizeExtractedDirectory(root: Path): Unit = {
    val list = Files.list(root).collect(Collectors.toList[Path])
    // Examine if the list only contains one directory. If it does, we need to move the directory content to the root
    if (list.size() == 1 && Files.isDirectory(list.get(0))) {
      Files.list(list.get(0)).forEach(path => Files.move(path, root.resolve(path.getFileName)))
      Files.delete(list.get(0)) // Delete empty directory
    }
  }

}
