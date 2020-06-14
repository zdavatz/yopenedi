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
// const nodemailer = require("nodemailer")
// const mailparser = require("mailparser")
//
/* -------------------------------------------------------------------------- */
/**
* Items Collection
    - Register/ Log every  process (email, conversion)
    - Validate read emails
*/

/* -------------------------------------------------------------------------- */
App = {}
App.path = process.env['METEOR_SHELL_DIR'] + '/../../../public/';
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
var messagesData = []
/* -------------------------------------------------------------------------- */
function getFiles() {
  // _.each(messages, (msg) => {
  for (i = 0; i < messages.length; i++) {
    var message = messages[i]
    if (!message) {
      return
    }
    console.log('GetFiles: Getting Message: ', message)
    var cmd = 'grab_one_message ' + message
    var isChecked = Items.findOne({
      message: message + "_"
    })
    //
    if (!isChecked) {
      console.log('Getting Message: ', message)
      var result = runCmd(cmd);
      messagesData[0] = result
      console.log('***** WRITING::::::: Message: ', message)
      fs.writeFileSync(project.edifact_orders + message + '_', messagesData[0], 'utf8', (err, result) => {
        if (err) {
          console.log(err);
        }
      });
      //
      var createdAt = downloadedAt = new Date()
      Items.insert({
        message: message + "_",
        createdAt: createdAt,
        downloadedAt: downloadedAt
      })
    } else {
      console.log('Message already checked', message)
    }
  }
  //
  console.log('======== ALL MESSAGES HAvE BEEN DOWNLOADED ==========')
  // Convert the Messages
  console.log('Processing Files: Converting to XML .....')
  project.processEdifactDir(project.edifact_orders)
  setTimeout(function () {
    console.log('Processing Files: Checking XML against https://connect.boni.ch .....')
    project.XMLCheck(project.opentrans_orders)
  }, 5000)
}
/* -------------------------------------------------------------------------- */
/*                       Reading emails                                       */
/* -------------------------------------------------------------------------- */
// return
var imap = new Imap({
  user: imapSettings.username,
  password: imapSettings.password,
  host: 'imap.gmail.com',
  port: 993,
  ssl: true,
  tls: true,
  tlsOptions: {
    rejectUnauthorized: false
  }
});
// return 
function openInbox(cb) {
  imap.openBox('INBOX', true, cb);
}
let getMailboxStatusByName = (mailServer, inboxName) => {
  mailServer.status(inboxName, (err, mailbox) => {
    console.log('message', mailbox);
  });
  console.log('message', 'Label or Box Status');
}
//
//
imap.once('ready', Meteor.bindEnvironment(function () {
  openInbox(Meteor.bindEnvironment(function (err, box) {
    if (err) throw err;
    var msgObj = {}
    // var f = imap.seq.fetch('1:3', {
    //     bodies: 'HEADER.FIELDS (FROM TO SUBJECT DATE)',
    //     struct: true
    // });
    //, , ['SINCE', 'October 2, 2013'] 
    //UNSEEN
    //ALL
    imap.search(['ALL'], Meteor.bindEnvironment(function (err, results) {
      if (err) {
        console.log('you are already up to date');
      }
      if (!results.length) {
        console.log('you are already up to date');
        return
      }
      /* -------------------------------------------------------------------------- */
      // Skip the search
      // var f = imap.seq.fetch('1:*', { bodies: '1:*', markSeen: true, struct: true ,  bodies: 'HEADER.FIELDS (FROM TO SUBJECT DATE)'});
      /* -------------------------------------------------------------------------- */
      var f = imap.fetch(results, {
        bodies: '',
        markSeen: true,
        struct: true,
        bodies: 'HEADER.FIELDS (FROM TO SUBJECT DATE)'
      });
      f.on('message', Meteor.bindEnvironment(function (msg, seqno) {
        // console.log('Message #%d', seqno);
        var prefix = '(#' + seqno + ') ';
        msg.on('body', function (stream, info) {
          // console.log('msg', info)
          var buffer = '';
          stream.on('data', function (chunk) {
            buffer += chunk.toString('utf8');
          });
          // Register the email
          stream.once('end', function () {
            // console.log(prefix + 'Parsed header: %s', inspect(Imap.parseHeader(buffer)));
            // console.log('Imap.parseHeader(buffer)',Imap.parseHeader(buffer))
            msgObj.header = Imap.parseHeader(buffer)
          });
        });
        //
        // Reading attachments
        msg.once('attributes', function (attrs) {
          // console.log('uid = ' + attrs.uid);
          messages.push(attrs.uid)
          return
          // console.log(prefix + 'Attributes: %s', inspect(attrs, false, 8));
          var attachments = findAttachmentParts(attrs.struct);
          console.log('==========', attachments)
          if (!attachments.length) {
            console.log('Skipped: The Email has no attachment')
            return
          } else {
            console.log('Attachments found:', attachments.length)
            msgObj.attachments = []
            msgObj.files = []
            msgObj.filesIds = ""
          }
          // console.log('ATTACHMENT:',attachments)
          for (var i = 0, len = attachments.length; i < len; ++i) {
            // PASS MESSAGE UUID to the attachment Object>>
            var attachment = attachments[i];
            console.log(prefix + 'Fetching attachment %s', attachment.params.name);
            var f = imap.fetch(attrs.uid, {
              bodies: [attachment.partID],
              struct: true
            });
            //build function to process attachment message
            msgObj.attachments.push(attachment)
            // console.log(JSON.stringify(attachment),null,2)
            msgObj.files.push({
              name: attachment.params.name,
              size: attachment.size,
              encoding: attachment.encoding
            })
            msgObj.msgId = msgObj.filesIds + attachment.id.replace(/[`~!@#$%^&*()_|+\-=?;:'",.<>\{\}\[\]\\\/]/gi, '');
            // console.log({attachment})
            // f.on('message', console.log('SUCCESS'));
            // return
            // USED TO WRITE THE FILE
            f.on('message', buildAttMessageFunction(attachment));
          }
        });
        //
        msg.once('end', Meteor.bindEnvironment(function () {
          //
          // msgObj.checkedAt = msgObj.createdAt = new Date()
          // msgObj.subject = msgObj.header.subject[0]
          // msgObj.date = msgObj.header.date[0]
          // msgObj.msgIdx = msgObj.filesIds + msgObj.header.date[0].replace(/\s/g, '');
          // console.log("*************", msgObj.msgIdx)
          // var isExist = Items.find({
          //   msgId: msgObj.msgId
          // }).fetch()
          // if (isExist.length == 0) {
          //   console.log('$$$$Found: New Email')
          //   Items.insert(msgObj, function (err) {
          //     if (!err) {
          //       console.log('Success: New Email Record %s', msgObj.subject)
          //     }
          //   })
          // } else {
          //   console.log('***************')
          //   console.log('Checking if exists:', isExist[0].msgId, " == ", msgObj.msgId)
          //   console.log('Checking if exists:', isExist[0].msgIdx, " == ", msgObj.msgIdx)
          //   console.log('Found:Exists Records', msgObj.subject, "*****************")
          //   console.log('***************')
          // }
          //
          // console.log(prefix + 'Finished, Checking count:', Items.find().count() );
        }));
        //
        //
      })); // end bind f.on
      f.once('error', function (err) {
        console.log('Fetch error: ' + err);
      });
      f.once('end', Meteor.bindEnvironment(function () {
        // console.log('Done fetching all messages!');
        // console.log('End, Emails count:', Items.find().count());
        console.log('Messeges: UIDs', {
          messages
        })
        getFiles()
        imap.end();
      }));
      // Get Unseen Inbox
    })) // get unseen
  })); // end bind
}));
imap.once('error', function (err) {
  console.log(err);
});
imap.once('end', function () {
  console.log('Connection ended');
});
App.checkMessages = function () {
  console.log('Checking Messages......')
  imap.connect();
}
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
/*                               Find Attachment                              */
/* -------------------------------------------------------------------------- */
function findAttachmentParts(struct, attachments) {
  attachments = attachments || [];
  for (var i = 0, len = struct.length, r; i < len; ++i) {
    if (Array.isArray(struct[i])) {
      findAttachmentParts(struct[i], attachments);
    } else {
      if (struct[i].disposition && ['INLINE', 'ATTACHMENT'].indexOf(struct[i].disposition.type) > -1) {
        attachments.push(struct[i]);
      }
    }
  }
  return attachments;
}
/* -------------------------------------------------------------------------- */
/*                    Build and Write Attachments to files                    */
/* -------------------------------------------------------------------------- */
function buildAttMessageFunction(attachment) {
  var filename = attachment.params.name;
  var encoding = attachment.encoding;
  console.log({
    filename,
    encoding
  })
  // return
  return function (msg, seqno) {
    var prefix = '(#' + seqno + ') ';
    msg.on('body', function (stream, info) {
      //Create a write stream so that we can stream the attachment to file;
      console.log(prefix + 'Streaming this attachment to file', filename, info);
      var writeStream = fs.createWriteStream(App.path + filename);
      writeStream.on('finish', function () {
        console.log(prefix + 'Done writing to file %s', filename);
      });
      if (encoding === 'BASE64') {
        //   stream.pipe(base64.decode()).pipe(writeStream);
        // stream.pipe(base64.decode()).pipe(writeStream);
        console.log('Found: buildAttMessageFunction: Base64 attachment found')
        stream.pipe(new Base64Decode()).pipe(writeStream);
      } else {
        console.log('Skipped: buildAttMessageFunction: Base64 attachment Skipped')
        stream.pipe(writeStream);
      }
    });
    msg.once('end', function () {
      console.log(prefix + 'Finished attachment %s', filename);
    });
  };
}
/* -------------------------------------------------------------------------- */
var log = {}
log.items = Items.find().fetch()
console.log("Items Length", log.items.length)
module.exports = App
// module.exports = files