import io.minio.{GetObjectResponse, StatObjectResponse}

import java.io.InputStream

trait CacheInterface {

  def bucketExists(bucket: String): Boolean
  def put(bucket: String, id: String, inputStream: InputStream, size: Long, contentType: String = "binary/octet-stream"): Unit
  def get(bucket: String, id: String): GetObjectResponse /* extends FilterInputStream */
  def delete(bucket: String, id: String): Unit
  def stat(bucket: String, id: String): StatObjectResponse
  
}
