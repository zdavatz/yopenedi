package models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class Message(var messageId: String, var requestSender: String, var requestRecipient: String, mic: String) {
  def mdnTextPart(): String = {
    val tz = TimeZone.getTimeZone("UTC")
    val  df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    df.setTimeZone(tz)
    val nowAsISO = df.format(new Date())

    "Content-Type: text/plain\r\n" +
    "Content-Transfer-Encoding: 7bit\r\n" +
    "\r\n" +
    "MDN for -\r\n" +
    " Message ID: " + messageId + "\r\n" +
    "  From: \"" + requestSender + "\"\r\n" +
    "  To: \"" + requestRecipient + "\"\r\n" +
    "  Received on: " + nowAsISO + "\r\n" +
    " Status: processed\r\n" +
    " Comment: This is not a guarantee that the message has\r\n" +
    "  been completely processed or &understood by the receiving\r\n" +
    "  translator\r\n"
  }

  def mdnMessagePart(): String = {
    "Content-Type: message/disposition-notification\r\n" +
    "Content-Transfer-Encoding: 7bit\r\n" +
    "\r\n" +
    "Reporting-UA: YOpenedi\r\n" +
    "Original-Recipient: rfc822; " + requestRecipient + "\r\n" +
    "Final-Recipient: rfc822; " + requestRecipient + "\r\n" +
    "Original-Message-ID: " + messageId + "\r\n" +
    "Received-content-MIC: " + mic + ", sha1\r\n" +
    "Disposition: automatic-action/MDN-sent-automatically;\r\n" +
    "  processed\r\n" +
    "\r\n"
  }

  def makeReport(boundary: String): String = {
    helpers.MultipartFormData.makeMultipartString(List(
      mdnTextPart(),
      mdnMessagePart()
    ), boundary)
  }

  def makeReportWithHeader(boundary: String): String = {
    "Content-Type: multipart/report;\r\n" +
    "Report-Type=disposition-notification; boundary=\"" + boundary + "\"\r\n" +
    makeReport(boundary)
  }
}
