import java.io.InputStream
import java.net.URLConnection
import java.util.zip.{Inflater, InflaterInputStream}
import org.apache.commons.io.input.TeeInputStream
import org.apache.commons.io.output.TeeOutputStream

import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.Character.MAX_RADIX
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import scala.concurrent.ExecutionContext
import scala.concurrent.Future


trait Utils {

  val networkTimeout = 50 * 1000 // ms

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // def md5(s: String) = MessageDigest.getInstance("MD5").digest(s.getBytes)

  def sha3384(s: String, radix: Int = MAX_RADIX): String = {
    val md = MessageDigest.getInstance("SHA3-384");
    new java.math.BigInteger(1, md.digest(s.getBytes(UTF_8))).toString(radix)
  }

  /** get URLConnection from url: String */
  def connect(
                    url: String,
                    timeout: Int = networkTimeout,
                    proxy: Option[java.net.Proxy] = None,
                    maxRedir: Int = 5
                  ): URLConnection =
  {
    if (maxRedir<=0)
      throw new Exception(s"Too many redirections. The last url: $url")

    val u = new java.net.URL(url)
    // convert to java.net.HttpURLConnection to fail when file:/ is used
    val connection = proxy.fold(u.openConnection())(u.openConnection(_)).asInstanceOf[java.net.HttpURLConnection]
    connection.setConnectTimeout(timeout)
    connection.setReadTimeout(timeout)
    // connection.getResponseCode() match case 301 | 302 => ...
    Option(connection.getHeaderField("Location")).fold {
      println(s"\treached at $url")
      connection
    }{ redirect =>
      println(s"\tredirected $redirect")
      connect(redirect, timeout, proxy, maxRedir-1)
    }
  }

  /** get InputStream from URLConnection */
  case class URLConnectionOps(connection: URLConnection) {
    def inputStream: InputStream = {
      connection.getContentEncoding() match {
        case "gzip" => new java.util.zip.GZIPInputStream(connection.getInputStream)
        case "deflate" => new InflaterInputStream(connection.getInputStream, new Inflater(true))
        case _ => connection.getInputStream
      }
    }
  }
  given Conversion[URLConnection, URLConnectionOps] = URLConnectionOps(_)

  /** get two InputStream from the one */
  case class InputStreamOps(is: InputStream) {

    // Not working well!
    /*
    def duplicate: List[InputStream] = {
      val pipedOuts = List.fill(2)(new PipedOutputStream)
      val inputStreams = pipedOuts.map(new PipedInputStream(_))
      val tout = new TeeOutputStream(pipedOuts(0), pipedOuts(1))
      val tin = new TeeInputStream(is, tout,  true)
      Future { tin.readAllBytes }
      inputStreams
    }
    */

    def iterator: Iterator[Int] = // in fact Iterator[Byte]
      Iterator.continually(is.read).takeWhile(_ != -1)

  }
  given Conversion[InputStream, InputStreamOps] = InputStreamOps(_)

  /**  */
  case class IteratorOps(it: Iterator[Int]) {
    def inputStream: InputStream =
      new InputStream {
        override def read(): Int = {
          if (it.hasNext) it.next() // need to convert Byte to unsigned int
          else -1
        }
      }
  }
  given Conversion[Iterator[Int], IteratorOps] = IteratorOps(_)
}
