/**
 * 
 */
import {
  WebApp
} from 'meteor/webapp'
import bodyParser from 'body-parser'
/* -------------------------------------------------------------------------- */

const AccessControlAllowOrigin = (req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  next()
}
// Access-Control-Allow-Origin
WebApp.connectHandlers.use(AccessControlAllowOrigin)
// parse application/x-www-form-urlencoded
WebApp.connectHandlers.use(bodyParser.urlencoded({
  extended: false
}))
// parse application/json
WebApp.connectHandlers.use(bodyParser.json())
// Listen to incoming HTTP requests (can only be used on the server).

/* -------------------------------------------------------------------------- */

WebApp.connectHandlers.use('/as2', (req, res, next) => {
  const json = req.method === 'POST' ? req.body || {} : {}
  console.log('API check: /as2')
  console.log('Header: ', JSON.stringify(req.headers));
  var file = JSON.stringify(req.body)
  console.log('File length: ', file.length)
  if (req.headers && req.headers["as2-to"] && req.headers['as2-from'] && file.length > 10 && req.headers["message-id"]) {
    console.log('Success: File Passed')
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(200);
  } else {
    console.log(JSON.stringify(req.headers));
    console.log('Error: File is not passed')
    res.writeHead(400)
  }
  res.end();
});