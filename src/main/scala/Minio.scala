import io.minio.*

import java.io.{ByteArrayInputStream, InputStream }

import scala.jdk.CollectionConverters._

object Minio extends CacheAPI {


  val minioClient = MinioClient.builder
    .endpoint("http://localhost:9000")
    .credentials(
      // generated at http://localhost:.../access-keys/new-account
      "J3ZPGJNYjja3z74T",
      "Gs82a3wW6APexLBT2cdQjXlmCwRa7mqk"
    )
    .build()

  import minioClient._

  def bucketExists(bucket: String): Boolean =
    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())

  /**
   * Put object into minio storage
   *
   * @param bucket bucket name
   * @param id     object id
   * @param blob   object blob
   */
  override def put(
                    bucket: String,
                    id: String,
                    inputStream: InputStream,
                    size: Long,
                    contentType: String = "binary/octet-stream"
                  ): Unit = {

    // create bucket if not exists
    if (!bucketExists(bucket)) {
      makeBucket(
        MakeBucketArgs.builder()
          .bucket(bucket)
          .build()
      )
    }

    putObject(
      PutObjectArgs.builder
        .bucket(bucket)
        .`object`(id)
        .stream(inputStream, size, -1)
        .contentType(contentType)
        .build
    )

    inputStream.close()
  }

  /**
   * Get object from minio storage
   *
   * @param bucket bucket name
   * @param id     object it
   */
  override def get(bucket: String, id: String): GetObjectResponse /* extends FilterInputStream */ =
    getObject(
      GetObjectArgs.builder
        .bucket(bucket)
        .`object`(id)
        .build
    )


  /**
   * remove object from minio storage
   *
   * @param bucket bucket name
   * @param id     object it
   */
  override def delete(bucket: String, id: String): Unit = {
    removeObject(
      RemoveObjectArgs.builder()
        .bucket(bucket)
        .`object`(id)
        .build()
    )

    println("bucket size\t= " + listObjects(ListObjectsArgs.builder().bucket(bucket).build()).asScala.size)
  }

  /**
   * get object info
   */
  override def stat(bucket: String, id: String): StatObjectResponse = {
    Minio.minioClient.statObject(
      StatObjectArgs.builder()
        .bucket(bucket)
        .`object`(id)
        .build()
    )
  }
}
