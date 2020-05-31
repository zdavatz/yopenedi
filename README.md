# yopenedi
OpenTrans 2.1/EDIFACT D.96A File converter
1. Convert order Files to and from OpenTrans 2.1/[EDIFACT D.96A](http://www.unece.org/trade/untdid/d96a/content.htm)\
Focus on the opening [UNH/UNT](https://user-images.githubusercontent.com/4953/82047602-5c4bb800-96b3-11ea-9b7d-5560743676dd.jpeg) Tags. They contain the order information like Buyer, Seller, Contact Address and itmes list. UNB is just the message header. Convert what is inside of UNH/UNT into Opentrans 2.1 XML.
* [Sample XML Files from OpenTrans 2.1](https://1drv.ms/u/s!AgbWsnOPcbiN7hNOj5J96OsS2svQ?e=OnPvPZ)
2. Send order files as attachment in EDIFACT D.96A format to xmlxonv@ywesee.com and convert them to the OpenTrans 2.1 format.
3. Import OpenTrans 2.1 files from FTP and convert them to EDIFACT D.96A and send the converted file as Email-Attachment from xmlxonv@ywesee.com to a dedicated Email address.
4. Send Opentrans 2.1 XML files to this [URL](https://connect.boni.ch/OpaccOne/B2B/Channel/XmlOverHttp/ywesee)
5. Receive Opentrans 2.1 XML files at this [URL](https://yopenedi.ch/input)

## FAQ
### EDIFACT D.96A
* [Stackoverflow](https://stackoverflow.com/questions/11295551/is-there-a-really-simple-way-to-process-edifact-for-example-d96a)\
** Just split at the sytax chars: first at `'` to get the segments, than at `+` to get data elements of that segments and at `:` to get the individual components.
* [edifact-parser](https://www.npmjs.com/package/edifact-parser)
* [Truugo](https://www.truugo.com/edifact/d96a/orders/)
* [Edicat](https://github.com/notpeter/edicat)
* [Edivisualizer](https://stackoverflow.com/questions/32889967/what-algorithm-to-use-to-format-an-edifact-file)
#### Useful Windows Tools
* [EDI Notepad](https://download.cnet.com/EDI-Notepad/3000-20411_4-76182031.html)
* [R2ediviewer](http://r2ediviewer.de/index.html)
### Opentrans 2.1
* [Opentrans 2.1 XSD](https://www.opentrans.de/XMLSchema/2.1/opentrans_2_1.xsd)
### AS2 Gateway
* https://medium.com/@manjulapiyumal/getting-start-with-as2-protocol-using-as2gateway-and-openas2-796249cfd3ac
* https://github.com/OpenAS2/OpenAs2App
* https://github.com/abhishek-ram/django-pyas2
### Send AS2 message with Curl
* https://stackoverflow.com/questions/42390232/edi-as2-http-trace
* https://docs.axway.com/bundle/B2Bi_230_AdministratorsGuide_allOS_en_HTML5/page/Content/Transports/Secure_file/curl_tool.htm

## Certbot for Ubuntu 20.04
* https://certbot.eff.org/lets-encrypt/ubuntufocal-other
* `certbot certonly --server https://acme-v02.api.letsencrypt.org/directory --manual --preferred-challenges dns -d 'test.yopenedi.ch'`

## Digital Ocean deployment
### Apache setup
```
<VirtualHost *:80>
  ServerName test.yopenedi.ch
  Redirect permanent / https://test.yopenedi.ch
</VirtualHost>

<VirtualHost 104.248.255.2:443>
  ServerName test.yopenedi.ch
  ProxyPreserveHost On
  ProxyPass  /excluded !
  ProxyPass / http://127.0.0.1:3000/
  ProxyPassReverse / http://127.0.0.1:3000/
  SSLEngine on
  SSLCertificateFile /etc/letsencrypt/live/test.yopenedi.ch/cert.pem
  SSLCertificateKeyFile /etc/letsencrypt/live/test.yopenedi.ch/privkey.pem
  SSLCertificateChainFile /etc/letsencrypt/live/test.yopenedi.ch/chain.pem
</VirtualHost>
```
