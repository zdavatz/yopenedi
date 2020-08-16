package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net._
import java.nio.charset.Charset
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import com.ywesee.java.yopenedi.converter._
import com.ywesee.java.yopenedi.Edifact._
import com.ywesee.java.yopenedi.OpenTrans._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, config: Configuration) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def as2Fn(request: Request[AnyContent]): Result = {
    val messageId = request.headers.get("message-id")
    val as2To = request.headers.get("as2-to")
    val as2From = request.headers.get("as2-from")
    val body = request.body.asText

    val now = LocalDateTime.now()
    val filename = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
    val encoding = request.charset.getOrElse("UTF-8")

    val ediOrdersPath = config.get[String]("edifact-orders")
    val otOrdersPath = config.get[String]("opentrans-orders")
    val environment = config.getOptional[String]("environment")

    val ediOrdersFolder = new File(ediOrdersPath)
    if (!ediOrdersFolder.exists()) {
      ediOrdersFolder.mkdirs()
    }
    val otOrdersFolder = new File(otOrdersPath)
    if (!otOrdersFolder.exists()) {
      otOrdersFolder.mkdirs()
    } else if (!otOrdersFolder.isDirectory()) {
      throw new Exception(otOrdersPath + " is not a directory");
    }

    def makeStream(): Either[Result, InputStream] = {
      request.body match {
        case c: AnyContentAsMultipartFormData =>
          if (c.mfd.files.length == 0) {
            Left(BadRequest("No file found"))
          } else {
            val file = c.mfd.files.head
            Right(new FileInputStream(file.ref.path.toFile()))
          }
        case c: AnyContentAsText =>
          Right(IOUtils.toInputStream(c.txt, encoding))
        case c: AnyContentAsRaw =>
          val file = c.raw.asFile
          Right(new FileInputStream(file))
        case _ =>
          Left(BadRequest("Invalid content type"))
      }
    }

    makeStream() match {
      case Left(r) =>
        return r
      case Right(s) =>
        val outFile = new File(ediOrdersFolder, filename)
        val ediOutStream = new FileOutputStream(outFile)
        IOUtils.copy(s, ediOutStream)
        Logger.debug("Edifact File size: " + outFile.length())
    }
    val outFile = new File(otOrdersFolder, filename)
    if (outFile.exists() && !outFile.canWrite()) {
      throw new Exception("Cannot write file to: " + outFile.getAbsolutePath())
    }

    var retried = false
    val result = retryWithEdifactExtracted(encoding, makeStream, (s: InputStream, isRetry) => {
      if (isRetry) {
        retried = true
      }
      val er = new EdifactReader()
      val ediOrders = er.run(s)
      Logger.debug("EDIFACT orders count: " + ediOrders.size())
      val converter = new Converter()
      if (ediOrders.size() == 0) {
        Left(BadRequest("No order found in EDIFACT file."))
      } else if (ediOrders.size() > 1) {
        Left(BadRequest("More than 1 order in EDIFACT file."))
      } else {
        val otOrder = converter.orderToOpenTrans(ediOrders.get(0))
        otOrder.isTestEnvironment = environment.equals(Some("test"))
        Logger.debug("Opentrans order: " + otOrder.toString())

        val configPath = config.getOptional[String]("conversion-config").getOrElse("./conf")
        val converterConfig = new Config(configPath)
        val writer = new OpenTransWriter(converterConfig)
        val outStream = new FileOutputStream(outFile)
        writer.write(otOrder, outStream)
        outStream.flush()
        outStream.getFD().sync()
        outStream.close()
        Logger.debug("File written: " + outFile.getAbsolutePath())
        Logger.debug("File size: " + outFile.length())
        Right(Unit)
      }
    })

    result match {
      case Left(e) => return e
      case _ => {}
    }

    if (retried) {
      makeStream() match {
        case Left(r) =>
          return r
        case Right(s) =>
          extractedEdifact(s, encoding) match {
            case Some(ediStr) =>
              val ediOutFile = new File(ediOrdersFolder, filename)
              FileUtils.write(ediOutFile, ediStr, encoding, false)
              Logger.debug("Edifact file size: " + ediOutFile.length())
            case _ => {}
          }
      }
    }

    val postToUrl = config.getOptional[String]("http-post-to")
    for (url <- postToUrl) {
      if (!url.isEmpty()) {
        uploadFile(url, outFile)
      }
    }

    result match {
      case Left(e) => return e
      case Right(_) =>
        val obj = Json.obj(
          "ok" -> true
        )
        return Ok(Json.toJson(obj))
    }
  }
  def as2() = Action(as2Fn(_))

  // Sometimes Play fails to parse request, e.g. it doesn't understand
  // the request when multipart boundary has "="
  // We retry when the stream fails, by manually extracting the EDIFACT out of the request body
  private def retryWithEdifactExtracted[A, B](encoding: String, mkStream: () => Either[A, InputStream], op: (InputStream, Boolean) => Either[A, B]): Either[A, B] = {
    mkStream().right.flatMap { s =>
      val result = op(s, false)
      result match {
        case Right(a) => Right(a)
        case Left(_) =>
          Logger.info("Retrying by extracting EDIFACT from request body")
          mkStream().right.flatMap { s =>
            var ediLine: Option[String] = extractedEdifact(s, encoding)
            ediLine match {
              case Some(l) => op(IOUtils.toInputStream(l, encoding), true)
              case None =>
                Logger.error("Cannot find EDIFACT from request body")
                op(IOUtils.toInputStream("", encoding), true)
            }
          }
      }
    }
  }

  private def extractedEdifact(s: InputStream, encoding: String): Option[String] = {
    val it = IOUtils.lineIterator(s, encoding)
    while (it.hasNext()) {
      val line = it.nextLine()
      if (line.startsWith("UNA") && line.contains("UNZ")) {
        return Some(line)
      }
    }
    return None
  }

  private def uploadFile(urlStr: String, file: File) {
    try {
      Logger.info("Uploading file (" + file.getAbsolutePath() + ") to " + urlStr);
      val url = new URL(urlStr);
      val con = url.openConnection();
      val http = con.asInstanceOf[HttpURLConnection];
      http.setRequestMethod("POST");
      http.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
      http.setDoInput(true);
      http.setDoOutput(true);
      IOUtils.copy(new FileInputStream(file), con.getOutputStream());
      val res = IOUtils.toString(con.getInputStream(), Charset.defaultCharset());
      Logger.info("Response: " + res);
    } catch {
      case e: Exception => Logger.error(e.getMessage());
    }
  }
}
