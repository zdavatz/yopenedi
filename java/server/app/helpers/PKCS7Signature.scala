package helpers

import java.io.FileInputStream
import java.security.PrivateKey
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.nio.file.{Files, Paths}
import java.util.Base64
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSProcessableInputStream
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder


import scala.collection.JavaConverters._


object PKCS7Signature {
  Security.addProvider(new BouncyCastleProvider())

  def sign(certPath: String, privateKeyPath: String, msgBytes: Array[Byte]): String = {
    val certStream = new FileInputStream(certPath);
    val cf = CertificateFactory.getInstance("X.509")
    val cert = cf.generateCertificate(certStream) match {
      case c: X509Certificate => c
      case _ =>
        throw new Exception("Not a X.509 cert")
    }
     certStream.close();

    val privateKey = getPrivateKey(privateKeyPath);

    val certList = List(cert)
    val certs = new JcaCertStore(certList.asJava)

    val sha1Signer: ContentSigner = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(privateKey);

    val gen = new CMSSignedDataGenerator();
    gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                     new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                     .build(sha1Signer, cert));
    gen.addCertificates(certs);
    val msg = new CMSProcessableByteArray(msgBytes);

    val signedData: CMSSignedData = gen.generate(msg, false);
    val pkcs7: Array[Byte] = signedData.getEncoded()
    var base64Encoder = new sun.misc.BASE64Encoder()
    val encoded = base64Encoder.encode(pkcs7)
    return encoded
  }

  def getPrivateKey(path: String): PrivateKey = {
    val keyBytes = Files.readAllBytes(Paths.get(path));
    val content = new String(keyBytes, "UTF-8").replaceAll("\\n|-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----", "");
    val spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(content));
    val kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(spec);
  }
}
