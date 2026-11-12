package controllers

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._

class AS2Spec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  val ediDir: File = Files.createTempDirectory("yopenedi-edifact-orders").toFile()
  val otDir: File = Files.createTempDirectory("yopenedi-opentrans-orders").toFile()

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("edifact-orders" -> ediDir.getAbsolutePath(), "opentrans-orders" -> otDir.getAbsolutePath())
    .build()

  def write(name: String, content: String): File = {
    val f = new File(ediDir, name)
    FileUtils.writeStringToFile(f, content, StandardCharsets.UTF_8)
    f
  }

  "POST /as2" should {
    // The MDN must not wait for the conversion: senders time out and resend.
    "return the MDN once the file is stored, before converting it" in {
      val request = FakeRequest(POST, "/as2")
        .withHeaders(HOST -> "localhost:9000", "message-id" -> "<test-1@example.com>", "as2-from" -> "SENDER", "as2-to" -> "YWESEE")
        .withTextBody("UNA:+.? 'UNB+UNOC:3+1:14+2:14+261111:1234+1'UNZ+1+1'")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsString(result) must include ("<test-1@example.com>")
      ediDir.listFiles().length must be >= 1
    }
  }

  "findEarlierCopy" should {
    "find an identical earlier file" in {
      val controller = inject[HomeController]
      val first = write("2026-11-11_123555", "UNA:+.? 'UNB+same'")
      first.setLastModified(System.currentTimeMillis() - 60000)
      write("2026-11-11_130000", "UNA:+.? 'UNB+other'")
      val again = write("2026-11-11_154607", "UNA:+.? 'UNB+same'")

      controller.findEarlierCopy(again).map(_.getName()) mustBe Some("2026-11-11_123555")
    }

    "not treat the first of two copies as a duplicate of the second" in {
      val controller = inject[HomeController]
      val first = write("2026-11-13_100000", "UNA:+.? 'UNB+twin'")
      write("2026-11-13_100001", "UNA:+.? 'UNB+twin'")

      controller.findEarlierCopy(first) mustBe None
    }

    "not treat a different order of the same size as a copy" in {
      val controller = inject[HomeController]
      write("2026-11-12_080000", "UNA:+.? 'UNB+aaaa'")
      val other = write("2026-11-12_090000", "UNA:+.? 'UNB+bbbb'")

      controller.findEarlierCopy(other) mustBe None
    }

    // 12.11.2026: an unreadable earlier file threw, and the order was not processed at all
    "process the order when an earlier file cannot be read" in {
      val controller = inject[HomeController]
      val earlier = write("2026-11-14_080000", "UNA:+.? 'UNB+lost'")
      val order = write("2026-11-14_090000", "UNA:+.? 'UNB+lost'")
      earlier.setReadable(false)
      try {
        controller.findEarlierCopy(order) mustBe None
      } finally {
        earlier.setReadable(true)
      }
    }
  }
}
