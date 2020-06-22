/**
 * 
 */
const fs = require('fs');
const fse = require('fs-extra')
const path = require('path');
// import Parse from './parse.draft.final.js'
var child_process = require('child_process');
const axios = require('axios');
const FormData = require('form-data');

log = console.log
/* -------------------------------------------------------------------------- */
const util = require('util');
const exec = util.promisify(require('child_process').exec);

/* -------------------------------------------------------------------------- */

import './collections.js'
import Parse from './parse.edi.js'
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
console.log('___init_IO___')
/* -------------------------------------------------------------------------- */
project = {}
project.path = process.env['METEOR_SHELL_DIR'] + '/../../../'
project.public = process.env['METEOR_SHELL_DIR'] + '/../../../public/';
project.private = project.path + '/private/'
/* -------------------------------------------------------------------------- */
project.edifact_orders = project.path + 'edifact_orders/'
project.opentrans_orders = project.path + 'opentrans_orders/'
project.edifact_orders_done = project.path + 'edifact_orders_done/'
project.edifact_orders_encryped = project.path + 'edifact_orders_encryped/'
/* -------------------------------------------------------------------------- */

files = {}
/* -------------------------------------------------------------------------- */
var filePath, fileSize;
var XMLCheckURL = 'https://connect.boni.ch/OpaccOne/B2B/Channel/XmlOverHttp/YWE'
checkFileCmd = 'curl -H "Content-Type: text/xml; charset=UTF-8" -H "Content-Length: ' + fileSize + '" ' + XMLCheckURL + '  --data-binary @' + filePath + ' -v'
/* -------------------------------------------------------------------------- */
project.emptyDir = function (dir) {
  console.log('==== Cleaning ' + dir + ' ======')
  fse.emptyDirSync(dir)
}
/* -------------------------------------------------------------------------- */
project.rm = function (path) {
  fs.unlink(path, (err) => {
    if (err) {
      console.error(err)
      return
    }
  })
}
/* -------------------------------------------------------------------------- */
project.XMLcheckFile = function (fileData) {
  var item = Items.findOne({
    message: fileData.name
  })
  //
  if (item && item.isChecked) {
    console.log('XMLCheck: File is already Checked', fileData.name)
    return
  } else {
    console.log('=========== Checking File: File is not checked: ', fileData.name)
    var fileSize = fileData.size;
    var filePath = fileData.filepath;

    // AXIOS function with Async.
    // xmlCheckAPI(fileData)
    // return 
    var checkFileCmd = 'curl -H "Content-Type: text/xml; charset=UTF-8" -H "Content-Length: ' + fileSize + '" ' + XMLCheckURL + '  --data-binary @' + filePath + ' -v -m 7'
    var checkXML = runCmd(checkFileCmd);
    // var checkXML =  runAsync(checkFileCmd,null)
    console.log('==== XML VALIDATION RESULT FOR ' + fileData.name, {
      checkXML
    })
    // Checking Message
    if (checkXML && isMsgSuccess(checkXML)) {
      console.log('Success:::https://connect.boni.ch: ', fileData.name)

      Items.update({
        message: fileData.name
      }, {
        $set: {
          isChecked: true,
          filename: fileData.name,
          filePath: filePath,
          fileSize: fileSize,
          apiResponse: "success:200",
          apiStatusCode: 200,
          isValid: true
        }
      })

    } else {
      console.error('Error:::https://connect.boni.ch :', fileData.name, " is returning an error")
      Items.update({
        message: fileData.name
      }, {
        $set: {
          isChecked: true,
          filename: fileData.name,
          filePath: filePath,
          fileSize: fileSize,
          apiResponse: "success:200",
          apiStatusCode: 400,
          isValid: false
        }
      })
      
    }

  }
}
/* -------------------------------------------------------------------------- */
// Checking XMLCheck.... 
project.XMLCheck = Meteor.bindEnvironment(function (dir) {
  console.log('===========Reading XML FILES ==============')
  readFiles(dir, Meteor.bindEnvironment(function (fileData) {
    console.log('=========== Checking File: ', fileData.name)
    //
    project.XMLcheckFile(fileData)
    //
  }));
});
/* -------------------------------------------------------------------------- */
project.ediToXML = function (fileData) {
  console.log('=========Processing File=====', fileData.name)
  var doc = fs.readFileSync(fileData.filepath, 'utf8');
  var xml = Parse.renderEDI(doc)
  // Write the translated file.
  var xmlPath = project.opentrans_orders + fileData.name
  //
  var item = Items.findOne({
    message: fileData.name
  })
  // console.log(fileData.name, item)
  // if (item && item.isConverted) {
  //   console.log('edifact File Coversion: File is already converted', fileData.name)
  //   return
  // } else {
  console.log('edifact file is processed and converted: ', fileData.name)
  console.log('---Writing File', fileData.name)
  writeFile(xmlPath + '.xml', xml)
  // Move the file to another folder
  writeFile(project.edifact_orders_done + fileData.name, doc)
  var xmlPath = project.opentrans_orders + fileData.name
  Items.update({
    message: fileData.name
  }, {
    $set: {
      isConverted: true,
      filename: fileData.name,
      xmlPath: xmlPath,
      fileSizeEdi: fileData.size
    }
  })
  // Close else
  // }
}
/* -------------------------------------------------------------------------- */

// #39 
// Order the files 
project.processEdifactDir = Meteor.bindEnvironment(function (dir) {

  readFiles(dir, Meteor.bindEnvironment(function (fileData) {
    console.log('filename:', fileData.name)
    project.ediToXML(fileData)
    // project.rm(fileData.filepath)
  }));


  //
})

/* -------------------------------------------------------------------------- */
function writeFile(file, data) {
  console.log('Writing file..........', file)
  fs.writeFileSync(file, data, 'utf8', (err, result) => {
    if (err) {
      console.log(err);
    }
  });
}
/* -------------------------------------------------------------------------- */

project.writeFile = function (file, data) {
  console.log('Writing file..........', file)
  fs.writeFileSync(file, data, 'utf8', (err, result) => {
    if (err) {
      console.log(err);
    }
  });
}
/* -------------------------------------------------------------------------- */
project.getFileData = function (file) {
  var fileData = {}
  fileData.name = path.parse(filename).name;
  fileData.ext = path.parse(filename).ext;
  fileData.filepath = path.resolve(dir, filename);
  // fileData.size = 
  fs.stat(fileData.filepath, function (error, stat) {
    if (error) throw error;
    var isFile = stat.isFile();
    // exclude folders
    fileData.size = stat.size
    if (isFile) {
      // callback, do something with the file
      processFile(fileData);
    }
  });
}
/* -------------------------------------------------------------------------- */
function readFiles(dir, processFile) {
  // read directory
  fs.readdir(dir, (error, fileNames) => {
    if (error) throw error;
    console.log({
      fileNames
    })
    fileNames.forEach(filename => {
      console.log({
        filename
      })
      var fileData = {}
      fileData.name = path.parse(filename).name;
      fileData.ext = path.parse(filename).ext;
      fileData.filepath = path.resolve(dir, filename);
      // fileData.size = 
      fs.stat(fileData.filepath, function (error, stat) {
        if (error) throw error;
        var isFile = stat.isFile();
        // exclude folders
        fileData.size = stat.size
        if (isFile) {
          // callback, do something with the file
          processFile(fileData);
        }
      });
    });
  });
}
/* -------------------------------------------------------------------------- */

project.writeOrder = function (folder, fileName, data) {
  var folder = folder ? folder : project.edifact_orders;
  var fileName = fileName
  fs.writeFileSync(folder + fileName, data, 'utf8', (err, result) => {
    if (err) {
      console.log(err);
    }
  });
}

/* -------------------------------------------------------------------------- */

/**
 * Running Command 
 * @param {} cmd 
 */
function runCmd(cmd) {
  console.log('Running Command: ', {
    cmd
  })
  var result = child_process.execSync(cmd);
  var result = result.toString('UTF8');
  console.log('runCmd: ',{result})
  return result;
}
/* -------------------------------------------------------------------------- */


/* -------------------------------------------------------------------------- */

/**
 * Check XML Message Success
 * @param {*} message 
 */
function isMsgSuccess(message) {
  var re = /(?:<Code>)([\s\S]*)(?:<\/Code>)/
  var result = message.match(re);
  if (result.length && result.length == 2 && result[1] == '200') {
    return true
  } else {
    return false
  }
}
/* -------------------------------------------------------------------------- */




/* -------------------------------------------------------------------------- */


function xmlCheckAPI(fileData) {



  // AXIOS function with Async.
  // xmlCheckAPI(fileData)
  // return 


  var fileSize = fileData.size;
  var filePath = fileData.filepath;
  // var checkFileCmd = 'curl -H "Content-Type: text/xml; charset=UTF-8" -H "Content-Length: ' + fileSize + '" ' + XMLCheckURL + '  --data-binary @' + filePath + ' -v'



  var form = new FormData();
  var stream = fs.createReadStream(filePath);

  form.append('file', stream);

  var api = 'http://localhost:3000/send'

  axios.post(api, form, {
      headers: {
        ...formHeaders,
      },
    })
    .then(response => response)
    .catch(error => error)
}


/* -------------------------------------------------------------------------- */
/**  */

async function runAsync(command, eventId) {
  var eventId = eventId?eventId: null;
  log('running command - runAsync', command, eventId)
  const {
    stdout,
    stderr
  } = await exec(command);

  o = {}
  o.log = stdout;
  if (stderr) {
    console.error(`error: ${stderr}`);
    o.log = stderr
  }
  o.finishedAt = new Date()
  o.isDone = true
  console.log(`Number of files ${stdout}`);

  // Events.update({
  //   _id: eventId
  // }, {
  //   $set: o
  // })

  return o;

}

/* -------------------------------------------------------------------------- */

module.exports = project
module.exports = files