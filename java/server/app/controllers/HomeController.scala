package controllers

import javax.inject._
import play.api._
import play.api.mvc._

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
    println(messageId)

    val now = LocalDateTime.now()
    val filename = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))

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
          Right(IOUtils.toInputStream(c.txt, request.charset.getOrElse("UTF-8")))
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

    makeStream() match {
      case Left(r) =>
        return r
      case Right(s) =>
        val er = new EdifactReader()
        val ediOrders = er.run(s)
        println(ediOrders)
        val converter = new Converter()
        if (ediOrders.size() == 0) {
          return BadRequest("More than 1 order in EDIFACT file.")
        }
        val otOrder = converter.orderToOpenTrans(ediOrders.get(0))
        println(otOrder)
        val writer = new OpenTransWriter()
        val outStream = new FileOutputStream(new File(otOrdersFolder, filename))
        writer.write(otOrder, outStream)
    }

    return Ok(views.html.index())
  }
  def as2() = Action(as2Fn(_))
}
