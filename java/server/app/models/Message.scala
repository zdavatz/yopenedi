package models

class Message(var messageId: String, var requestSender: String, var requestRecipient: String) {
  def mdnTextPart(): String = {
    "Content-Type: text/plain\r\n" +
    "Content-Transfer-Encoding: 7bit\r\n" +
    "\r\n" +
    "MDN for -\r\n" +
    " Message ID: " + messageId + "\r\n" +
    "  From: \"" + requestSender + "\"\r\n" +
    "  To: \"" + requestRecipient + "\"\r\n" +
    "  Received on: 2002-07-31 at 09:34:14 (EDT)\r\n" +
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
    "Received-content-MIC: 7v7F++fQaNB1sVLFtMRp+dF+eG4=, sha1\r\n" +
    "Disposition: automatic-action/MDN-sent-automatically;\r\n" +
    "  processed\r\n" +
    "\r\n"
  }

  def makeReport(boundary: String): String = {
    "Content-Type: multipart/report;\r\n" +
    "Report-Type=disposition-notification;\r\n" +
    "boundary=\"" + boundary + "\"\r\n" +
    helpers.MultipartFormData.makeMultipartString(List(
      mdnTextPart(),
      mdnMessagePart()
    ), boundary)
  }
}
