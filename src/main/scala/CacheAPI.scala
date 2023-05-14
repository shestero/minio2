import java.io.InputStream

trait CacheAPI {

  def put(bucket: String, id: String, inputStream: InputStream, size: Long, contentType: String = "binary/octet-stream"): Unit
  def get(bucket: String, id: String): (String, InputStream) // ("Content-Type") -> content 
  def delete(bucket: String, id: String): Unit
  def stat(bucket: String, id: String): Map[String, String]

}
