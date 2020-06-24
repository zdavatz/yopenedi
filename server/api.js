/**
 * 0- decrypt 
 * 1- save the edifact file
 * 2- convert
 * 3- save 
 * 
 * --- 
 * - Check file: (edi,encrypted,text)
 * - 
 */
import {
  WebApp
} from 'meteor/webapp'
import bodyParser from 'body-parser'
const nodersa = require('node-rsa');
const fs = require('fs');
const moment = require('moment');
const path = require('path');
/* -------------------------------------------------------------------------- */
const multer = require('multer');
const upload = multer();
/* -------------------------------------------------------------------------- */
// PICKER MIDDLE WARE
Picker.middleware(upload.any());
Picker.middleware( bodyParser.json() );
Picker.middleware( bodyParser.urlencoded( { extended: false } ) );
// Picker.middleware( bodyParser.text({type: '/'}) );
/* -------------------------------------------------------------------------- */

import './edi.js';
import './io.js'
/* -------------------------------------------------------------------------- */
const settings = Meteor.settings;
// Setting Private Key.
// const privateKey = fs.readFileSync(settings.private.private_key, 'utf8');
/* -------------------------------------------------------------------------- */
const AccessControlAllowOrigin = (req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  next()
}
// Access-Control-Allow-Origin
WebApp.connectHandlers.use(AccessControlAllowOrigin)
// parse application/x-www-form-urlencoded
WebApp.connectHandlers.use(bodyParser.urlencoded({
  extended: true
}))
// parse application/json
WebApp.connectHandlers.use(bodyParser.json())
// Listen to incoming HTTP requests (can only be used on the server).
/* -------------------------------------------------------------------------- */
// Active
/* -------------------------------------------------------------------------- */
Picker.route('/as2', function (params, req, res, next) {
  console.log(req.body)
  var fileData = {}
  var msg = {}
  msg.id = req.headers["message-id"] ? req.headers["message-id"] : null
  msg.to = req.headers["as2-to"] ? req.headers["as2-to"] : null
  msg.from = req.headers['as2-from'] ? req.headers['as2-from'] : null
  // Check the headers.
  if (!req.headers || !msg.id || !msg.to || !msg.from) {
    console.log('Missing Header data')
    res.writeHead(400, {
      'Content-Type': 'application/json'
    })
    fileData.status = 400;
    fileData.message = 'Error: Check Headers and F field'
    res.end(JSON.stringify(fileData));
  }


  // --data-binary

  if(req && req.body){
    
    var keys = Object.keys(req.body);
    var data = keys[0];
    console.log('data:body',data)

    var responseData = {}
    if(data && data.length){
      res.writeHead(200, {
        'Content-Type': 'application/json'
      })
      responseData.message = 'Data is ready'
      responseData.length = data.length;
      console.log(responseData)

    }else{
      res.writeHead(400, {
        'Content-Type': 'application/json'
      })
      responseData.message = 'There is no binary data'
      console.log(responseData)
    }
    res.end(JSON.stringify(responseData));
  }


  // -F option 
  // Process the file
  if (req.files && req.files.length > 0 && req.headers && msg.id && msg.to && msg.from) {
    console.log('File: is valid (-F) ', req.files[0])
    var file = req.files[0];
    var outputPath = project.edifact_orders_encryped + file.originalname
    console.log('Success file upload at:', outputPath)
    writeFile(outputPath, file.buffer)
    // deencrypt 
    // var ediFileData = new nodersa(privateKey).decrypt(file.data, 'utf8');
    // console.log('DeencryptedData: ',{ediFileData})
    //
    res.writeHead(200, {
      'Content-Type': 'application/json'
    })
    fileData.status = 200;
    fileData.message = 'File is uploaded'
    fileData.headers = JSON.stringify(req.headers)
    fileData.data = {
      name: file.originalname,
      mimetype: file.mimetype,
      size: file.size
    }
    res.end(JSON.stringify(fileData));
  }
  // -T option
  let body = ''
  req.on('data', Meteor.bindEnvironment((data) => {
    body += data;
  })).on('end', function () {
    //
    var doc = body;
    console.log({
      doc
    })
    // -T option 
    if (doc) {
      console.log('headers: -T ', JSON.stringify(req.headers))
      var filename = setFileName();
      var outputPath = project.edifact_orders_encryped + filename
      writeFile(outputPath, doc)
      console.log('Success (-T): File is converted')
      res.writeHead(200, {
        'Content-Type': 'application/json'
      })
      
      fileData.data = {
        name:filename,
        size: req["content-length"]
      }
      res.end(JSON.stringify(fileData));
    } 

    // If No DOCUMENT or No File uploaded
    if(!body && (req.files && req.files.length == 0) || (!req.files && !body)){
      console.log('Error: There is no file or document detected')
      res.writeHead(400, {
        'Content-Type': 'application/json'
      })
      message = {
        status: 400,
        message: "There is no file or document detected",
        headers : JSON.stringify(req.headers)
      }
      res.end(JSON.stringify(message));
    }
  })
})
/* -------------------------------------------------------------------------- */
// 
/* -------------------------------------------------------------------------- */
WebApp.connectHandlers.use('/as', (req, res, next) => {
  const json = req.method === 'POST' ? req.body || {} : {}
  console.log('API check: /as2')
  console.log('Header: ', JSON.stringify(req.headers));
  console.log(req.body)
  console.log('file: ', req.files)
  //
  var file = req.body
  // var type = req.get('Content-Type');
  console.log('File length: ', JSON.stringify(file).length)
  var dataLength = JSON.stringify(file).length
  //file.length > 10 &&
  var msg = {}
  msg.id = req.headers["message-id"]
  msg.to = req.headers["as2-to"]
  msg.from = req.headers['as2-from']
  // + "_" + new Date();
  msg.fileName = msg.id
  if (req.headers && msg.id && msg.to && msg.from && dataLength > 100) {
    console.log('Success: File Passed')
    ediProcess(JSON.stringify(req.body), msg)
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(200);
    res.statusCode = 200
  } else {
    console.log(JSON.stringify(req.headers));
    console.log('Error: File is not passed')
    res.statusCode = 400
    res.writeHead(400)
  }
  res.end();
});
/* -------------------------------------------------------------------------- */
// WebApp.connectHandlers.use(MultipartParser);
// Picker.middleware(_multerInstance.single('file'));
Picker.route('/send', function (params, req, res, next) {
  console.log('SEND', req.headers)
  let body = ''
  req.on('data', Meteor.bindEnvironment((data) => {
    body += data;
  })).on('end', function () {
    var doc = body;
    console.log({
      doc
    })
    if (doc) {
      console.log('Success: File is converted')
      res.writeHead(200, {
        'Content-Type': 'text/html'
      })
      // res.statusCode = 200
      res.end();
    } else {
      console.log('Success: File is NOT converted')
      res.writeHead(400, {
        'Content-Type': 'text/html'
      })
      // res.statusCode = 200
      res.end();
    }
  })
})
/* -------------------------------------------------------------------------- */
function writeFile(outputPath, data) {
  fs.writeFileSync(outputPath, data, "binary", (err) => {
    if (err) {
      console.log('wWiteFile: (api)', {
        err
      });
    } else {
      console.log('Success (api): File is written:', file.name)
      console.log('File location: ', outputPath)
    }
  });
}
/* -------------------------------------------------------------------------- */

function ediProcess(doc, msg) {
  // Writing Edifact File
  console.log(doc)
  project.writeOrder(project.edifact_orders, msg.fileName, doc)
  // var xml = Parse.renderEDI(doc);
}
/* -------------------------------------------------------------------------- */

// set file name 

function setFileName(){
  // var format = "dd.mm.ss-yyyy"
  // HH:mm:ss_dd:mm:yyyy
  return moment().format("HH_mm_ss_DD-MM-YYYY");
}