/**
 * 
 */
import {
  WebApp
} from 'meteor/webapp'
import bodyParser from 'body-parser'
import Parse from './parse.edi.js'
import './edi.js';
import './io.js'
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
WebApp.connectHandlers.use('/as', (req, res, next) => {
  const json = req.method === 'POST' ? req.body || {} : {}
  console.log('API check: /as2')
  console.log('Header: ', JSON.stringify(req.headers));
  //JSON.stringify(req.body)
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
  } else {
    console.log(JSON.stringify(req.headers));
    console.log('Error: File is not passed')
    res.writeHead(400)
  }
  res.end();
});
/* -------------------------------------------------------------------------- */
Picker.route('/as2', function (params, req, res, next) {
  let body = ''
  req.on('data', Meteor.bindEnvironment((data) => {
    body += data;
  })).on('end', function () {
    var msg = {}
    msg.id = req.headers["message-id"]
    msg.to = req.headers["as2-to"]
    msg.from = req.headers['as2-from']
    let d = new Date()
    let ye = new Intl.DateTimeFormat('en', {
      year: 'numeric'
    }).format(d)
    let mo = new Intl.DateTimeFormat('en', {
      month: 'short'
    }).format(d)
    let da = new Intl.DateTimeFormat('en', {
      day: '2-digit'
    }).format(d)
    // + "_" + new Date();
    msg.fileName = msg.id + "_" + `${da}_${mo}_${ye}`
    console.log(JSON.stringify(req.headers))
    console.log(body)
    // var dataLength = JSON.stringify(body).length
    var doc = body;
    // Writing A Message 
    if (req.headers && msg.id && msg.to && msg.from && body) {
      console.log('Success: File Passed')
      res.setHeader('Content-Type', 'application/json');
      res.writeHead(200);
      project.writeOrder(project.edifact_orders_encryped, msg.fileName, doc)

      project.writeOrder(project.edifact_orders, msg.fileName, doc)
      var xml = Parse.renderEDI(doc)
      console.log('Converted to XML', {
        xml
      })
      project.rm(project.edifact_orders + msg.fileName)
      project.writeOrder(project.opentrans_orders, msg.fileName + ".xml", xml)
      project.writeOrder(project.edifact_orders_done, msg.fileName, doc)

      console.log('Success: File is converted')
    } else {
      console.log(JSON.stringify(req.headers));
      console.log('Error: File is not passed')
      res.writeHead(400)
    }
    res.end();
  })
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