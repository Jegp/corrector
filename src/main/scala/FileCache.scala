import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

/**
  * A file cache for storing Strings over time.
  */
class FileCache(root: Path) {

  def erase(): Unit = {
    FileUtils.deleteDirectory(root.toFile)
  }

  def get(id: String): Future[String] = {
    Future {
      Source.fromFile(root.resolve(id).toFile).mkString
    }
  }

  def put(key: String, value: String): Unit = {
    val filePath = root.resolve(key)
    if (!Files.exists(filePath)) {
      Files.createFile(filePath)
    }
    Files.write(root.resolve(key), value.getBytes("UTF-8"))
  }

}

object FileCache {

  def apply(): FileCache = new FileCache(Files.createTempDirectory("filecache"))

}
