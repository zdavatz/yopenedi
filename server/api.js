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
import Parse from './parse.edi.js'
const nodersa = require('node-rsa');
const fs = require('fs');
const moment = require('moment');
const path = require('path');
const exec = require('child_process').exec;
const cmd = Meteor.wrapAsync(exec);
/* -------------------------------------------------------------------------- */
const multer = require('multer');
const upload = multer();
/* -------------------------------------------------------------------------- */
// PICKER MIDDLE WARE
Picker.middleware(upload.any());
Picker.middleware( bodyParser.json() );
Picker.middleware( bodyParser.urlencoded( { extended: true } ) );
// Picker.middleware( bodyParser.text({type: '/'}) );
/* -------------------------------------------------------------------------- */
import './edi.js';
import './io.js'
/* -------------------------------------------------------------------------- */
const settings = Meteor.settings;
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
//
// WebApp.connectHandlers.use(MultipartParser);
// Listen to incoming HTTP requests (can only be used on the server).
// 
/* -------------------------------------------------------------------------- */
// Active
/* -------------------------------------------------------------------------- */
Picker.route('/as2', function (params, req, res, next) {
  var fileData = {}
  var msg = {}
  msg.id = req.headers["message-id"] ? req.headers["message-id"] : null
  msg.to = req.headers["as2-to"] ? req.headers["as2-to"] : null
  msg.from = req.headers['as2-from'] ? req.headers['as2-from'] : null
  // Check the headers.
  // if (!req.headers || !msg.id || !msg.to || !msg.from) {
  //   console.log('Missing Header data')
  //   res.writeHead(400, {
  //     'Content-Type': 'application/json'
  //   })
  //   fileData.status = 400;
  //   fileData.message = 'Error: Check Headers and F field'
  //   res.end(JSON.stringify(fileData));
  // }
  // --data-binary
  if(req && req.body){
    console.log('REQUEST BODY',req.body)
    var keys = Object.keys(req.body);
    var data = keys[0];
    console.log('data:body',data)
    var responseData = {}
    if(data && data.length){
      res.writeHead(200, {
        'Content-Type': 'application/json'
      })
      responseData.status ="SUCCESS";
      responseData.message = 'Data is ready'
      responseData.length = data.length;
      console.log({responseData})
      var filename = setFileName();
      var outputPath = project.edifact_orders_encryped + filename
      fileData.filepath = outputPath;
      fileData.name = filename
      console.log('Success file upload at:', outputPath,data)
      var doc = extractEdiFactData(data)
      writeFile(outputPath, doc)
      res.end(JSON.stringify(responseData));
    }
  }
  // -F option 
  // Process the file
  if (req.files && req.files.length > 0 && req.headers && msg.id && msg.to && msg.from) {
    console.log('File: is valid (-F) ', req.files[0])
    var file = req.files[0];
    var outputPath = project.edifact_orders_encryped + file.originalname
    fileData.filepath = outputPath;
    fileData.name = file.originalname
    console.log('Success file upload at:', outputPath)
    var doc = extractEdiFactData(file.buffer)
    writeFile(outputPath, doc)
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
  //===============================================================//
  // -T option
  let body = ''
  req.on('data', Meteor.bindEnvironment((data) => {
    body += data;
  })).on('end', Meteor.bindEnvironment(function () {
    //
    var doc = body;
    console.log({
      doc
    })
    ////
    // -T option 
    if (doc) {
      console.log('headers: -T ', JSON.stringify(req.headers))
      var filename = setFileName();
      var outputPath = project.edifact_orders_encryped + filename
      var doc = extractEdiFactData(doc)
      writeFile(outputPath, doc)
      fileData.filepath = outputPath;
      fileData.name = filename;
      console.log('-T',{fileData})
      console.log('Success (-T): File is converted')
      res.writeHead(200, {
        'Content-Type': 'application/json'
      })
     data = {
        status: "SUCCESS",
        code: 200,
        message: "File is uploaded",
        name:filename,
        size: req.headers["content-length"]
      }
      res.end(JSON.stringify(data));
      convertFile(fileData)
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
  }))
  // Convert the File and Run XML Validation Check.
  if((req.files && req.files.length > 0 ) || (req && req.body && JSON.stringify(req.body).length > 200) ){
    convertFile(fileData)
  }
})
/* -------------------------------------------------------------------------- */

// var getAS2EdiData = '091124_01072020'
// var ediData = Assets.getText(getAS2EdiData)
// extractEdiFactData(ediData)


function extractEdiFactData(data){
  if(!data){
    console.error("There is No Data in the submitted file")
    return
  }
  var message = data.match(/UNA(.*)UNZ/ig)
  var tail = data.match(/UNZ(.*)'/ig)
  var message = message + tail;
  console.log("Extracting Edifact Message From The Binary Message: ",{message})
  return message;

}



/* -------------------------------------------------------------------------- */
function convertFile(fileData){
  console.log('Converting the file: (convertFile)',JSON.stringify(fileData))
  var doc = fs.readFileSync(fileData.filepath, 'utf8');
  var xml = Parse.renderEDI(doc)
  var xmlPath = project.opentrans_orders + fileData.name
  console.log('SUCCESS: File is Converted at: ',xmlPath)
  writeFile(xmlPath + ".xml" ,xml)
  // Write Backup file at edifact_orders_done
  writeFile(project.edifact_orders_done + fileData.name, doc)
  // File Validation
  // Re-set FileData
  var xmlFilePath = xmlPath + '.xml'
  // Get the file size
  var stats = fs.statSync(xmlFilePath)
  console.log("stats", stats.size)
  fileData.size = stats.size;
  fileData.filepath = xmlFilePath;
  fileData.name = fileData.name + '.xml'
  project.XMLcheckFile(fileData)
}
/* -------------------------------------------------------------------------- */
// Check if it's an Edifact file
function isEdiFile(fileContent){
  if (fileContent.substring(0, 3) !== "UNA") {
    console.error("The Document is not valid Edifact file", "getFile")
    return;
  }else{
    return true
  }
}
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
  return moment().format("HHmmss_DDMMYYYY");
}
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
