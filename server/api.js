/**
 * 
 */
import {
  WebApp
} from 'meteor/webapp'
import bodyParser from 'body-parser'
const nodersa = require('node-rsa');
const fs = require('fs');
const fse = require('fs-extra')
const path = require('path');
/* -------------------------------------------------------------------------- */

const multer = require('multer');
const upload = multer();
Picker.middleware(upload.any());
/* -------------------------------------------------------------------------- */

import Parse from './parse.edi.js'
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


//   let body = ''
//   req.on('data', Meteor.bindEnvironment((data) => {
//     body += data;
//   })).on('end', function () {
//     var doc = body;
//     console.log({doc})

//     console.log('Success: File is converted')
//     res.writeHead(200, {
//       'Content-Type': 'text/html'
//     })
//     // res.statusCode = 200
//     res.end();

//   })





var fileData = {}
var msg = {}
msg.id = req.headers["message-id"]?req.headers["message-id"]:null
msg.to = req.headers["as2-to"]?req.headers["as2-to"]:null
msg.from = req.headers['as2-from']?req.headers['as2-from']:null



// Validate Headers
if (!req.headers || !msg.id || !msg.to || !msg.from) {
  console.log('Missing Header data')
  res.writeHead(200, {
    'Content-Type': 'application/json'
  })
  res.writeHead(400, {
    'Content-Type': 'application/json'
  })
  fileData.status = 400;
  fileData.message = 'Error: Check Headers and F field'
  res.end(JSON.stringify(fileData));
}
// Process the file
if (req.files && req.files.length > 0 && req.headers && msg.id && msg.to && msg.from) {

  console.log('File: ', req.files[0])
  var file = req.files[0];
  var outputPath = project.edifact_orders_encryped + file.originalname
  fs.writeFileSync(outputPath, file.buffer, "binary", (err, result) => {
    if (err) {
      console.log(err);
    } else {
      console.log('Success: File is written:', file.name)
    }
  });

  console.log('Success file upload at:', outputPath)
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
} else {
  res.writeHead(400, {
    'Content-Type': 'application/json'
  })
  fileData.status = 400;
  fileData.message = 'Error: Check Headers and F field'
  res.end(JSON.stringify(fileData));
}
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
  console.log('SEND',req.headers)

  if (req.files && req.files.length > 0) {
    console.log(req.files[0])
    var file = req.files[0];
    var outputPath = project.edifact_orders_encryped + file.originalname
    fs.writeFileSync(outputPath, file.data, "binary", (err, result) => {
      if (err) {
        console.log(err);
      } else {
        console.log('Success: File is written:', file.name)
      }
    });
    res.writeHead(200, {
      'Content-Type': 'application/json'
    })
    var fileData = {
      status: true,
      message: 'File is uploaded',
      data: {
        name: file.originalname,
        mimetype: file.mimetype,
        size: file.size
      }
    }
    res.end(JSON.stringify(fileData));
  }else{
    console.log('FILE IS NOT DEFINED')
  }
})
/* -------------------------------------------------------------------------- */
/**
 * 0- decrypt 
 * 1- save the edifact file
 * 2- convert
 * 3- save 
 */
function ediProcess(doc, msg) {
  // Writing Edifact File
  console.log(doc)
  project.writeOrder(project.edifact_orders, msg.fileName, doc)
  // var xml = Parse.renderEDI(doc);
}