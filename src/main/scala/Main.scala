import java.io.ByteArrayInputStream
import cats.effect.*
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder
import sttp.tapir.*
import sttp.tapir.files.{FilesOptions, staticFilesGetServerEndpoint}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.model.HeaderNames.ContentType
import sttp.model.MediaType.{ApplicationOctetStream,TextPlainUtf8}

import java.net.URI
import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.lang.Character.MAX_RADIX
import java.security.MessageDigest
import scala.util.Try
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration


object Main extends IOApp with Utils {

  val bucketName = "web-resources"

  val logic: String => IO[Either[Unit, (String,InputStream)]] = url => {
    val hash = sha3384(url)
    println(s"Asking for ($hash):\t$url")

    IO {
      val cached = Minio.get(bucketName, hash)
      println(s"Cached!\t$hash\t$url")
      Right( cached.headers().get("Content-Type") -> cached.asInstanceOf[InputStream] )
    } orElse IO {
      println(s"Not in cache:\t$hash\t$url")

      Try {
        val connection = connect(url)

        val contentLengthOpt = Option(connection.getContentLength).filter(_ >= 0) // -1 if unknown
        val contentType = Option(connection.getContentType) getOrElse ApplicationOctetStream.toString
        println("contentLength=" + contentLengthOpt)
        println("contentType=" + contentType)

        //val List(toClient, toCache) = connection.inputStream.duplicate

        Future {
          // save to Minio
          // do some delay here
          val connection = connect(url)
          val contentLengthOpt = Option(connection.getContentLength).filter(_ >= 0) // -1 if unknown
          val contentType = Option(connection.getContentType) getOrElse ApplicationOctetStream.toString
          contentLengthOpt match {
            case None => // length unknown
              val blob: Array[Byte] = connection.inputStream.readAllBytes() // download all
              Minio.put(bucketName, hash, new ByteArrayInputStream(blob), blob.length, contentType)
            case Some(length) =>
              Minio.put(bucketName, hash, connection.inputStream, length, contentType)
          }
          println(s"Saved in cache ($hash)")

        }.failed.foreach { error =>
          println(s"Cannot cache ($hash)\t$url\t"+error.getMessage)
        }

        contentType -> connection.inputStream
      }.recover { error =>
          TextPlainUtf8.toString -> new ByteArrayInputStream(error.getMessage.getBytes(UTF_8))
      }.toOption.toRight[Unit](())
    }
  }

  val options: FilesOptions[IO] =
    FilesOptions
      .default
      .withUseGzippedIfAvailable // serves file.txt.gz instead of file.txt if available and Accept-Encoding contains "gzip"
      .defaultFile(List("index.html"))

  val rootEndpoint = staticFilesGetServerEndpoint(emptyInput)("./www", options)

  val cacheEndpoint =
    endpoint.description("get from cached url")
      .get
      .in("cache" / query[String]("url"))
      .out(header(ContentType)(Codec.listHead(Codec.string))) // dynamic content type
      .out(inputStreamBody)
      .serverLogic(logic)

  /*
  // delete object
  println("delete")
  Minio.delete("bucketName", "01234")
  */

  val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes( List(cacheEndpoint, rootEndpoint) )

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(9090, "0.0.0.0")
      .withHttpApp(Router("/" -> routes).orNotFound)
      .resource
      .useForever
      .as(ExitCode.Success)
}
