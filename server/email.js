/**
 * 
 * Email Connect
 * 
 */
const fs = require('fs')
const Imap = require("imap")
inspect = require('util').inspect;
import './collections.js'
import './io.js'
var {
  Base64Decode
} = require('base64-stream');
var child_process = require('child_process');
import _ from 'lodash';
import e from 'express';
const {
  execSync
} = require('child_process');
//
/* -------------------------------------------------------------------------- */
/**
* Items Collection
    - Register/ Log every  process (email, conversion)
    - Validate read emails
*/
/* -------------------------------------------------------------------------- */
App = {}
/* -------------------------------------------------------------------------- */
var settings = Meteor.settings
if (!settings || !settings.private || !settings.private.imap) {
  throw new Meteor.Error('setting-err', 'Setting file is not loaded')
} else {
  var imapSettings = settings.private.imap
  if (!imapSettings.username || !imapSettings.password) {
    throw new Meteor.Error('emai-error', 'Username or password are missing')
  }
  console.log("Success:", "Setting has been loaded")
  console.log("Test:Username", imapSettings.username)
}
/* -------------------------------------------------------------------------- */
function runCmd(cmd) {
  var resp = child_process.execSync(cmd);
  var result = resp.toString('UTF8');
  return result;
}
/* -------------------------------------------------------------------------- */
var messages = []
var messageData = []
/* -------------------------------------------------------------------------- */
/*                       Reading emails                                       */
/* -------------------------------------------------------------------------- */
//
imap = new Imap({
  user: imapSettings.username,
  password: imapSettings.password,
  host: 'imap.gmail.com',
  port: 993,
  ssl: true,
  tls: true,
  tlsOptions: {
    rejectUnauthorized: false
  },
  keepalive: {
    interval: 3600,
    idleInterval: 3600,
    forceNoop: true
  }
});
function openInbox(callback) {
  imap.openBox('INBOX', true, callback);
};
var getMail = Meteor.bindEnvironment(function () {
  imap.once('ready', function () {
    openInbox(function (err, box) {
      if (err) throw err;
      imap.search(['ALL'], function (err, results) {
        if (err) throw err;
        var f = imap.fetch(results, {
          bodies: ''
        });
        f.on('message', function (msg, seqno) {
          // console.log('Message #%d', seqno);
          var prefix = '(#' + seqno + ') ';
          // msg.on('body', function (stream, info) {
          // });
          msg.once('attributes', function (attrs) {
            var isNewMessage = _.includes(messages, attrs.uid)
            if (!isNewMessage) {
              console.log(`New Message detected: ${attrs.uid}`)
            }
            messages.push(attrs.uid)
            messages = _.uniq(messages)
            // console.log(prefix + 'Attributes: %s', inspect(attrs, false, 8));
          });
          msg.once('end', function () {
            console.log(prefix + 'Finished');
          });
        });
        f.once('error', function (err) {
          console.log('Fetch error: ' + err);
        });
        f.once('end', function () {
          console.log('Done fetching all messages');
          imap.end();
        });
      });
    });
  })
  //
  imap.once('error', function (err) {
    console.log(err);
  });
  //
  imap.once('end', Meteor.bindEnvironment(function () {
    console.log('Connection closed');
    getFiles()
  }));
  imap.connect();
});
function getFile(msg) {
  console.log('Grabbing Message file {getFile}: ', msg)

  var cmd = 'grab_one_message ' + msg
  var result = runCmd(cmd)
  if (!result) {
    console.log('There is no Message')
    return
  }
  if (result.substring(0, 3) !== "UNA") {
    console.error("The Document is not valid Edifact file", "getFile")
    return
  }
  if (result && result.length < 200) {
    console.error('There is an error while reading the file for: ', cmd)
    return
  }
  console.log('Reading Message Success: ', msg)
  fs.writeFileSync(project.edifact_orders + msg + '_', result, 'utf8', (err, result) => {
    if (err) {
      console.log(err);
    }
  });
  return result;
}
/* -------------------------------------------------------------------------- */
function getFiles() {
  // _.each(messages, (msg) => {
  console.log('Getting Messages {get Messages}', {
    messages
  })
  for (i = 0; i < messages.length; i++) {
    var message = messages[i]
    if (!message) {
      return
    }
    console.log('GetFiles: Getting Message: ', message)
  
    var isChecked = Items.findOne({
      message: message + "_"
    })
    //
    if (!isChecked) {
      console.log('Getting Message: ', message)
      getFile(message)
      //
      var createdAt = downloadedAt = new Date()
      Items.insert({
        message: message + "_",
        createdAt: new Date(),
        downloadedAt: new Date()
      })
    } else {
      console.log('Message already downloaded: ', message)
    }
  }
  //
  console.log('======== ALL MESSAGES HAE BEEN DOWNLOADED &  Ready To Convert. ==========')
  // Convert the Messages
  console.log('Processing Files: Converting to XML .....')
  project.processEdifactDir(project.edifact_orders)
  setTimeout(function () {
    console.log('Processing Files: Checking XML against https://connect.boni.ch .....')
    project.XMLCheck(project.opentrans_orders)
  }, 5000)
}
/* -------------------------------------------------------------------------- */

function runCmd(cmd) {
  console.log('runCmd', {
    cmd
  })
  try {
    var resp = child_process.execSync(cmd);
    var result = resp.toString('UTF8');
    return result;
    //
  } catch (err) {
    console.log('Err: Grabbing Message', cmd)
    return
  }
}
// TESTING MESSAGES....
// _.each([64,42,55,34,433],(i)=>{
//   var result = runCmd('grab_one_message '+i)
//   if(result && result.length < 200){
//     console.error('There is an error while reading the file for: ', cmd)
//     return
//   }else if(!result){      
//     console.log('There is no Message')
//     return
//   }else{
//     console.log('Reading Message Success:',{result})
//   }
//   console.log({result})
// })
module.exports = getMail;
// return