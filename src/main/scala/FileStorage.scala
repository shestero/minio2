import io.minio.{GetObjectResponse, StatObjectResponse}

import java.io.{File, FileInputStream, FileOutputStream, InputStream}
import java.nio.file.{Files, Path}

class FileStorage extends CacheAPI {

  val dir = "./storage"

  override def put(bucket: String, id: String, inputStream: InputStream, size: Long, contentType: String = "binary/octet-stream"): Unit = {
    val bucketPath = s"$dir/$bucket"
    // create it if not exists
    val f = new File(bucketPath)
    if (!f.exists()) f.mkdir()

    val fileName = s"$bucketPath/$id"
    val output = Path.of(fileName)
    Files.copy(inputStream, output)
  }

  override def get(bucket: String, id: String): (String, InputStream) = {
    val fileName = s"$dir/$bucket/$id"
    "binary/octet-stream" -> new FileInputStream(fileName)
  }

  override def delete(bucket: String, id: String): Unit = {
    // TODO
  }

  override def stat(bucket: String, id: String): Map[String, String] = {
    Map.empty // TODO
  }

}
