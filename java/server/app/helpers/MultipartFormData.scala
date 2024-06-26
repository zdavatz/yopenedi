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
    val content = elements.map(value => {
      actualBoundary + HTTP_SEPARATOR +
      value + HTTP_SEPARATOR
    }).mkString + endBoundary
    return content
  }
}
