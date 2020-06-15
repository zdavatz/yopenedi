/**
 * 
 */
import {
  WebApp
} from 'meteor/webapp'
import bodyParser from 'body-parser'
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

WebApp.connectHandlers.use('/as2', (req, res, next) => {
  const json = req.method === 'POST' ? req.body || {} : {}
  console.log('API check: /as2')
  console.log('Header: ', JSON.stringify(req.headers));
  //JSON.stringify(req.body)
  var file = req.body 
  // var type = req.get('Content-Type');
  console.log('File length: ', file)
  //file.length > 10 &&


  if (req.headers && req.headers["as2-to"] && req.headers['as2-from'] &&  req.headers["message-id"]) {
    console.log('Success: File Passed')
    ediProcess(file)
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


Picker.route('/as', function(params, req, res, next) {
  let body = ''
  req.on('data', Meteor.bindEnvironment((data) => {
    body += data;
  })).on('end', function() {
      console.log(params,req)
      console.log(body)
      // do something here
     res.end('api result');
  })
})



/* -------------------------------------------------------------------------- */
/**
 * 0- decrypt 
 * 1- save the edifact file
 * 2- convert
 * 3- save 
 */

function ediProcess(doc){
  // Writing Edifact File

  console.log(doc)
  // project.writeOrder(project.edifact_orders,'Hello',doc)
  // var xml = Parse.renderEDI(doc);

}