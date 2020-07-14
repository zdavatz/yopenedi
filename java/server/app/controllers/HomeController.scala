package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import com.ywesee.java.yopenedi.converter._
import com.ywesee.java.yopenedi.Edifact._
import com.ywesee.java.yopenedi.OpenTrans._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

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

  def as2() = Action { implicit request: Request[AnyContent] =>
    val messageId = request.headers.get("message-id")
    val as2To = request.headers.get("as2-to")
    val as2From = request.headers.get("as2-from")
    val body = request.body.asText
    println(messageId)

    var stream: Option[InputStream] = None
    val wsWithBody = request.body match {
      case c: AnyContentAsText =>
        stream = Some(IOUtils.toInputStream(c.txt, "UTF-8"))

      case c: AnyContentAsRaw =>
        val file = c.raw.asFile
        stream = Some(new FileInputStream(file))
      case _ =>
        println("no")
    }
    stream match {
      case Some(s) =>
        val er = new EdifactReader()
        val ediOrders = er.run(s)
        println(ediOrders)
        val converter = new Converter()
        val otOrder = converter.orderToOpenTrans(ediOrders.get(0))
        println(otOrder)
        val writer = new OpenTransWriter()
        val outStream = new FileOutputStream("/Users/b123400/github/yopenedi/java/testout")
        writer.write(otOrder, outStream)
      case None =>
        println("invalid content type")
    }
    Ok(views.html.index())
  }
}
