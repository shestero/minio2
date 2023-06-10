import io.minio.{GetObjectResponse, StatObjectResponse}

import java.io.{File, FileInputStream, FileOutputStream, InputStream, PrintWriter}
import java.nio.file.{Files, Path}
import scala.io.Source
import scala.util.Try

class FileStorage extends CacheAPI {

  val dir = "./storage"
  val defaultContentType = "binary/octet-stream"

  override def put(bucket: String,
                   id: String, inputStream: InputStream,
                   size: Long,
                   contentType: String = defaultContentType
                  ): Unit = {
    val bucketPath = s"$dir/$bucket"
    // create it if not exists
    val f = new File(bucketPath)
    if (!f.exists()) f.mkdir()

    val fileName = s"$bucketPath/$id"
    // Files.copy(inputStream, Path.of(fileName)) // consider not safe

    val outputStream = new FileOutputStream(fileName)
    val channel = outputStream.getChannel()

    Try {
      val lock = channel.tryLock() // can throw OverlappingFileLockException
      if (lock!=null) {
        inputStream.transferTo(outputStream)
        lock.release()
      } else {
        System.err.println(s"File $fileName is locked. Cannot write into it.")
      }
    }.failed.foreach { e =>
      System.err.println(s"Failed to write into file $fileName; error=${e.getMessage}")
    }
    outputStream.close()
    channel.close()

    // save contentType
    new PrintWriter(s"$fileName.mime") {
      write(contentType)
      close()
    }
  }

  override def get(bucket: String, id: String): (String, InputStream) = {
    val fileName = s"$dir/$bucket/$id"
    val contentType = Source.fromFile(s"$fileName.mime").getLines().nextOption() getOrElse defaultContentType
    contentType -> new FileInputStream(fileName)
  }

  override def delete(bucket: String, id: String): Unit = {
    // TODO
  }

  override def stat(bucket: String, id: String): Map[String, String] = {
    Map.empty // TODO
  }

}
