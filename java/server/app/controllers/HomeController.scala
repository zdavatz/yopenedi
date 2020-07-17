package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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

    val ediOrdersFolder = new File(ediOrdersPath)
    if (!ediOrdersFolder.exists()) {
      ediOrdersFolder.mkdirs()
    }
    val otOrdersFolder = new File(otOrdersPath)
    if (!otOrdersFolder.exists()) {
      otOrdersFolder.mkdirs()
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
        val ediOutStream = new FileOutputStream(new File(ediOrdersFolder, filename))
        IOUtils.copy(s, ediOutStream)
    }

    val result = retryWithEdifactExtracted(encoding, makeStream, (s: InputStream) => {
      val er = new EdifactReader()
      val ediOrders = er.run(s)
      println(ediOrders)
      val converter = new Converter()
      if (ediOrders.size() == 0) {
        Left(BadRequest("No order found in EDIFACT file."))
      } else if (ediOrders.size() > 1) {
        Left(BadRequest("More than 1 order in EDIFACT file."))
      } else {
        val otOrder = converter.orderToOpenTrans(ediOrders.get(0))
        println(otOrder)
        val writer = new OpenTransWriter()
        val outStream = new FileOutputStream(new File(otOrdersFolder, filename))
        writer.write(otOrder, outStream)
        Right(Unit)
      }
    })

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
  def retryWithEdifactExtracted[A, B](encoding: String, mkStream: () => Either[A, InputStream], op: InputStream => Either[A, B]): Either[A, B] = {
    mkStream().right.flatMap { s =>
      val result = op(s)
      result match {
        case Right(a) => Right(a)
        case Left(_) =>
          mkStream().right.flatMap { s =>
            val it = IOUtils.lineIterator(s, encoding)
            var ediLine: Option[String] = None
            while (it.hasNext()) {
              val line = it.nextLine()
              if (line.startsWith("UNA") && line.contains("UNZ")) {
                ediLine = Some(line)
              }
            }
            ediLine match {
              case Some(l) => op(IOUtils.toInputStream(l, encoding))
              case None => op(IOUtils.toInputStream("", encoding))
            }
          }
      }
    }
  }
}
