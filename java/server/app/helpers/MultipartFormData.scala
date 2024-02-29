package helpers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

object MultipartFormData {
  def makeMultipartString(elements: Seq[String], boundary: String): String = {
    val HTTP_SEPARATOR = "\r\n"
    val actualBoundary = "--" + boundary
    val endBoundary = actualBoundary + "--" + HTTP_SEPARATOR
    val contentType = "multipart/form-data; boundary=" + boundary
    val content = elements.map(value => {
      // val (name, value) = nameValuePair
      actualBoundary + HTTP_SEPARATOR +
      // "Content-Disposition: form-data; name=\"" + name + "\"" + HTTP_SEPARATOR +
      // HTTP_SEPARATOR +
      value + HTTP_SEPARATOR
    }).mkString + endBoundary
    return content
  }
}
