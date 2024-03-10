package controllers

import javax.inject._
import play.api.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.ws._

import play.api.libs.streams._
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net._
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.Base64

import com.ywesee.java.yopenedi.common._
import com.ywesee.java.yopenedi.converter._
import com.ywesee.java.yopenedi.Edifact._
import com.ywesee.java.yopenedi.OpenTrans._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, config: Configuration, ws: WSClient) extends AbstractController(cc) {
  val logger: Logger = Logger(this.getClass())
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

    val now = LocalDateTime.now()
    val filename = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))

    val environment = config.getOptional[String]("environment")
    val isTestEnvironment = environment.equals(Some("test"))
    val configPath = config.getOptional[String]("conversion-config").getOrElse("./conf")
    val converterConfig = new Config(configPath, isTestEnvironment)
    val ediOrdersPath = config.get[String]("edifact-orders")
    val otOrdersPath = config.get[String]("opentrans-orders")

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
          Right(IOUtils.toInputStream(convertedText(request.charset, c.txt), "UTF-8"))
        case c: AnyContentAsRaw =>
          val file = c.raw.asFile
          Right(new FileInputStream(file))
        case _ =>
          Left(BadRequest("Invalid content type"))
      }
    }

    var recipientGLN : String = null
    val result = retryWithEdifactExtracted(makeStream, (s: InputStream, isRetry) => {
      // We have to save the file first, and then read from the file, instead of making a new stream from request
      // otherwise it hits this bug:
      // http://milyn.996300.n3.nabble.com/Character-Encoding-problem-td1055.html
      val ediFile = new File(ediOrdersFolder, filename)
      val ediOutStream = new FileOutputStream(ediFile)
      IOUtils.copy(s, ediOutStream)
      logger.debug("Edifact File size: " + ediFile.length())

      val outFile = new File(otOrdersFolder, filename)
      if (outFile.exists() && !outFile.canWrite()) {
        throw new Exception("Cannot write file to: " + outFile.getAbsolutePath())
      }

      val er = new EdifactReader()
      val ediInStream = new FileInputStream(ediFile)
      val ediOrders = er.run(ediInStream)
      logger.debug("EDIFACT orders count: " + ediOrders.size())
      val converter = new Converter(converterConfig)
      if (ediOrders.size() == 0) {
        Left(BadRequest("No order found in EDIFACT file."))
      } else if (ediOrders.size() > 1) {
        Left(BadRequest("More than 1 order in EDIFACT file."))
      } else {
        val otOrder = converter.orderToOpenTrans(ediOrders.get(0))
        otOrder.isTestEnvironment = isTestEnvironment
        logger.debug("Opentrans order: " + otOrder.toString())

        val recipient = otOrder.getRecipient()
        if (recipient != null) {
          recipientGLN = recipient.id
        }

        val writer = new OpenTransWriter(converterConfig)
        val outStream = new FileOutputStream(outFile)
        writer.write(otOrder, outStream)
        outStream.flush()
        outStream.getFD().sync()
        outStream.close()
        logger.debug("File written: " + outFile.getAbsolutePath())
        logger.debug("File size: " + outFile.length())
        converterConfig.dispatchResult(recipientGLN, "ORDERS", outFile, messageId.getOrElse(""))
        Right(())
      }
    })

    val mic = makeReceivedContentMIC(request)

    val notificationOptions = request.headers.get("Disposition-notification-options")
    val isSigned = canSign() && (notificationOptions match {
      case None => false
      case Some(optionsStr: String)=> {
        val options = optionsStr.split(";")
        options.exists((option: String) => {
          val parts = option.split("=")
          if (parts.length == 2) {
            val key = parts(0)
            val value = parts(1)
            if (key.trim() == "signed-receipt-protocol") {
              value.trim().contains("pkcs7-signature")
            } else {
              false
            }
          } else {
            false
          }
        })
      }
    })

    result match {
      case Left(e) => return e
      case Right(_) =>
        val message = new models.Message(
          messageId.getOrElse("MISSING MESSAGE ID"),
          as2From.getOrElse("MISSING as2From"),
          as2To.getOrElse("MISSING as2To"),
          mic
        )
        request.headers.get("Receipt-Delivery-Option") match {
          case Some(url) => {
            if (isSigned) {
              makeAsyncSignedMDNRequest(message, url, request.headers)
            } else {
              makeAsyncUnsignedMDNRequest(message, url, request.headers)
            }
          }
          case None => {}
        }
        if (isSigned) {
          return makeSignedMDNResult(message)
        }
        return makeUnsignedMDNResult(message)
    }
  }

  def makeUnsignedMDNResult(message: models.Message): Result = {
    val body = message.makeReport("-----mdnboundarystring1")
    return Ok(body).as("multipart/report; Report-Type=disposition-notification; boundary=\"-----mdnboundarystring1\"")
  }

  def makeSignedMDNResult(message: models.Message): Result = {
    val body = message.makeReportWithHeader("-----mdnboundarystring1")
    val signaturePart = "Content-Type: application/pkcs7-signature; name=smime.p7s\r\n" +
      "Content-Transfer-Encoding: base64\r\n" +
      "Content-Disposition: attachment; filename=smime.p7s\r\n\r\n" +
      sign(body.getBytes(StandardCharsets.UTF_8))
    val data = helpers.MultipartFormData.makeMultipartString(List(body, signaturePart), "-----mdnboundarystring2")
    return Ok(data).as("multipart/signed; micalg=sha1; protocol=\"application/pkcs7-signature\"; boundary=\"-----mdnboundarystring2\"")
  }

  def makeAsyncUnsignedMDNRequest(message: models.Message, url: String, headers: Headers) = {
    val boundary = "-----mdnboundarystring1"
    val request: WSRequest = ws.url(url)
    request
      .addHttpHeaders("Message-ID" -> message.messageId)
      .addHttpHeaders("Recipient-Address" -> url)
      .addHttpHeaders("AS2-To" -> message.requestRecipient)
      .addHttpHeaders("AS2-From" -> message.requestSender)
      .addHttpHeaders("Subject" -> "Your Requested MDN Response")
      .addHttpHeaders("Content-Type" -> ("multipart/report; Report-Type=disposition-notification; boundary=\"" + boundary + "\""))
      .post(message.makeReport(boundary))
  }

  def makeAsyncSignedMDNRequest(message: models.Message, url: String, headers: Headers) = {
    val outerBoundary = "-----mdnboundarystring1"
    val innerBoundary = "-----mdnboundarystring2"
    val request: WSRequest = ws.url(url)
      .addHttpHeaders("Message-ID" -> message.messageId)
      .addHttpHeaders("Recipient-Address" -> url)
      .addHttpHeaders("AS2-To" -> message.requestRecipient)
      .addHttpHeaders("AS2-From" -> message.requestSender)
      .addHttpHeaders("Subject" -> "Your Requested MDN Response")
      .addHttpHeaders("Content-Type" -> ("multipart/signed; micalg=sha1; protocol=\"application/pkcs7-signature\"; boundary=\"" + outerBoundary + "\""))
    val body = message.makeReportWithHeader(innerBoundary)
    val signaturePart = "Content-Type: application/pkcs7-signature; name=smime.p7s\r\n" +
      "Content-Transfer-Encoding: base64\r\n" +
      "Content-Disposition: attachment; filename=smime.p7s\r\n\r\n" +
      sign(body.getBytes(StandardCharsets.UTF_8))
    val data = helpers.MultipartFormData.makeMultipartString(List(body, signaturePart), outerBoundary)
    request.post(data)
  }

  def makeReceivedContentMIC(request: Request[AnyContent]): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    request.body match {
      case c: AnyContentAsMultipartFormData =>
        val file = c.mfd.files.head
        val content = Files.readAllBytes(file.ref.path)
        val sig = new String(Base64.getEncoder().encode(md.digest(content.toArray)))
        return sig
      case c: AnyContentAsText =>
        val sig = new String(Base64.getEncoder().encode(md.digest(convertedText(request.charset, c.txt).getBytes(StandardCharsets.UTF_8))))
        return sig
      case c: AnyContentAsRaw =>
        val file = c.raw.asFile
        val bytes = new Array[Byte](1024 * 1024)
        val fis = new FileInputStream(file)
        LazyList.continually(fis.read(bytes)).takeWhile(-1 != _).foreach(md.update(bytes, 0, _))
        val sig = new String(Base64.getEncoder().encode(md.digest()))
        return sig
      case _ =>
        Left(BadRequest("Invalid content type"))
    }
    null
  }

  def as2() = Action(as2Fn(_))

  def canSign(): Boolean = {
    try {
      val certPath = config.get[String]("cert")
      val keyPath = config.get[String]("private-key")
      val certFile = new File(certPath)
      if (!certFile.exists() || certFile.isDirectory()) {
        return false
      }
      val keyFile = new File(keyPath)
      if (!keyFile.exists() || keyFile.isDirectory()) {
        return false
      }
      return true
    } catch {
      case _ : Throwable => return false
    }
  }

  def sign(data: Array[Byte]): String = {
    helpers.PKCS7Signature.sign(
      config.get[String]("cert"),
      config.get[String]("private-key"),
      data,
    )
  }

  def janicoFn(request: Request[AnyContent]): Result = {
    val now = LocalDateTime.now()
    val filename = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))

    val environment = config.getOptional[String]("environment")
    val isTestEnvironment = environment.equals(Some("test"))
    val configPath = config.getOptional[String]("conversion-config").getOrElse("./conf")
    val converterConfig = new Config(configPath, isTestEnvironment)
    val ediOrderResponsesPath = config.get[String]("edifact-order-responses")
    val otOrderResponsesPath = config.get[String]("opentrans-order-responses")
    val ediDespatchAdvicesPath = config.get[String]("edifact-despatch-advices")
    val otDispatchNotificationsPath = config.get[String]("opentrans-dispatch-notifications")
    val ediInvoicesPath = config.get[String]("edifact-invoices")
    val otInvoicesPath = config.get[String]("opentrans-invoices")

    val ediOrderResponsesFolder = new File(ediOrderResponsesPath)
    if (!ediOrderResponsesFolder.exists()) {
      ediOrderResponsesFolder.mkdirs()
    }
    val otOrderResponsesFolder = new File(otOrderResponsesPath)
    if (!otOrderResponsesFolder.exists()) {
      otOrderResponsesFolder.mkdirs()
    }
    val ediDespatchAdvicesFolder = new File(ediDespatchAdvicesPath)
    if (!ediDespatchAdvicesFolder.exists()) {
      ediDespatchAdvicesFolder.mkdirs()
    }
    val otDispatchNotificationsFolder = new File(otDispatchNotificationsPath)
    if (!otDispatchNotificationsFolder.exists()) {
      otDispatchNotificationsFolder.mkdirs()
    }
    val ediInvoicesFolder = new File(ediInvoicesPath)
    if (!ediInvoicesFolder.exists()) {
      ediInvoicesFolder.mkdirs()
    }
    val otInvoicesFolder = new File(otInvoicesPath)
    if (!otInvoicesFolder.exists()) {
      otInvoicesFolder.mkdirs()
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
          Right(IOUtils.toInputStream(convertedText(request.charset, c.txt), StandardCharsets.UTF_8))
        case c: AnyContentAsRaw =>
          val file = c.raw.asFile
          Right(new FileInputStream(file))
        case c: AnyContentAsXml =>
          Right(IOUtils.toInputStream(convertedText(request.charset, c.xml.toString()), StandardCharsets.UTF_8))
        case _ =>
          Left(BadRequest("Invalid content type"))
      }
    }

    var edifactType: String = null
    var orderId: String = null
    var recipientGLN: String = null
    var ediFolder: File = null
    var otFolder: File = null
    var outFile: File = null
    makeStream() match {
      case Left(r) =>
        return r
      case Right(s) =>
        val converter = new Converter(converterConfig)
        val writable = converter.run(s).snd

        if (writable.isInstanceOf[com.ywesee.java.yopenedi.Edifact.OrderResponse]) {
          val r = writable.asInstanceOf[com.ywesee.java.yopenedi.Edifact.OrderResponse]
          edifactType = "ORDRSP"
          orderId = r.documentNumber
          ediFolder = ediOrderResponsesFolder
          otFolder = otOrderResponsesFolder
          val recipient = r.getRecipient()
          if (recipient != null) {
            recipientGLN = recipient.id
          }
        } else if (writable.isInstanceOf[com.ywesee.java.yopenedi.Edifact.DespatchAdvice]) {
          val r = writable.asInstanceOf[com.ywesee.java.yopenedi.Edifact.DespatchAdvice]
          edifactType = "DESADV"
          orderId = r.documentNumber
          ediFolder = ediDespatchAdvicesFolder
          otFolder = otDispatchNotificationsFolder
          val recipient = r.getRecipient()
          if (recipient != null) {
            recipientGLN = recipient.id
          }
        } else if (writable.isInstanceOf[com.ywesee.java.yopenedi.Edifact.Invoice]) {
          val r = writable.asInstanceOf[com.ywesee.java.yopenedi.Edifact.Invoice]
          edifactType = "INVOIC"
          orderId = r.documentNumber
          ediFolder = ediInvoicesFolder
          otFolder = otInvoicesFolder
          val recipient = r.getRecipient()
          if (recipient != null) {
            recipientGLN = recipient.id
          }
        } else {
          return BadRequest("Invalid file type")
        }

        outFile = new File(ediFolder, filename)
        if (outFile.exists() && !outFile.canWrite()) {
          throw new Exception("Cannot write file to: " + outFile.getAbsolutePath())
        }

        val otOutStream = new FileOutputStream(outFile)
        writable.write(otOutStream, converterConfig)
        logger.debug("Wrote file to: " + outFile.getAbsolutePath())
    }

    val inFile = new File(otFolder, filename)
    if (inFile.exists() && !inFile.canWrite()) {
      throw new Exception("Cannot write file to: " + inFile.getAbsolutePath())
    }

    makeStream() match {
      case Left(r) =>
        return r
      case Right(s) =>
        val otStream = new FileOutputStream(inFile)
        IOUtils.copy(s, otStream)
        logger.debug("OpenTrans File size: " + inFile.length())
    }

    converterConfig.dispatchResult(recipientGLN, edifactType, outFile, orderId)

    val obj = Json.obj(
      "ok" -> true
    )
    return Ok(Json.toJson(obj))
  }
  def janico() = Action(janicoFn(_))

  // Sometimes Play fails to parse request, e.g. it doesn't understand
  // the request when multipart boundary has "="
  // We retry when the stream fails, by manually extracting the EDIFACT out of the request body
  private def retryWithEdifactExtracted[A, B](mkStream: () => Either[A, InputStream], op: (InputStream, Boolean) => Either[A, B]): Either[A, B] = {
    mkStream().flatMap { s =>
      val result = op(s, false)
      result match {
        case Right(a) => Right(a)
        case Left(_) =>
          logger.info("Retrying by extracting EDIFACT from request body")
          mkStream().flatMap { s =>
            var ediLine: Option[String] = extractedEdifact(s)
            ediLine match {
              case Some(l) => op(IOUtils.toInputStream(l, "UTF-8"), true)
              case None =>
                logger.error("Cannot find EDIFACT from request body")
                op(IOUtils.toInputStream("", "UTF-8"), true)
            }
          }
      }
    }
  }

  private def extractedEdifact(s: InputStream): Option[String] = {
    val it = IOUtils.lineIterator(s, "UTF-8")
    while (it.hasNext()) {
      val line = it.nextLine()
      if (line.startsWith("UNA") && line.contains("UNZ")) {
        return Some(line)
      }
    }
    return None
  }

  private def convertedText(encoding: Option[String], input: String): String = {
    encoding match {
      case None => new String(input.getBytes("ISO-8859-1"), "UTF-8")
      case Some(enc) => new String(input.getBytes(enc), "UTF-8")
    }
  }
}
