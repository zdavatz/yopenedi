/**
 * 
 */
const fs = require('fs');
const fse = require('fs-extra')
const path = require('path');
// import Parse from './parse.draft.final.js'

var child_process = require('child_process');

import Parse from './parse.edi.js'

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
files = {}


/* -------------------------------------------------------------------------- */

var filePath, fileSize;
var XMLCheckURL = 'https://connect.boni.ch/OpaccOne/B2B/Channel/XmlOverHttp/YWE'
checkFileCmd  = 'curl -H "Content-Type: text/xml; charset=UTF-8" -H "Content-Length: '+fileSize+'" ' +XMLCheckURL+ '  --data-binary @'+filePath+' -v'

/* -------------------------------------------------------------------------- */

project.emptyDir = function (dir) {
  console.log('==== Cleaning ' + dir + ' ======')
  fse.emptyDirSync(dir)
}


project.rm = function (path) {

  fs.unlink(path, (err) => {
    if (err) {
      console.error(err)
      return
    }

  })

}

/* -------------------------------------------------------------------------- */

project.XMLCheck = function(dir,func){
  console.log('===========Reading XML FILES ==============')
  readFiles(dir, (fileData)=>{
    console.log('=========== XML FILE ==============',fileData.name)
    var fileSize = fileData.size;
    var filePath = fileData.filepath;
    var checkFileCmd  = 'curl -H "Content-Type: text/xml; charset=UTF-8" -H "Content-Length: '+fileSize+'" ' +XMLCheckURL+ '  --data-binary @'+filePath+' -v'
    var checkXML = runCmd(checkFileCmd);
    console.log('==== XML VALIDATION RESULT FOR '+ fileData.name, {checkXML})
  });
}

/* -------------------------------------------------------------------------- */


project.processEdifactDir = function (dir, func) {
  readFiles(dir, (fileData) => {

    console.log('=========Processing File=====',fileData.name)
    var doc = fs.readFileSync(fileData.filepath, 'utf8');
    var xml = Parse.renderEDI(doc)
    console.log('---Writing File', fileData.name)
    // Write the translated file.
    writeFile(project.opentrans_orders + fileData.name + '.xml', xml)
    // Move the file to another folder
    writeFile(project.edifact_orders_done + fileData.name, doc)
    // project.rm(fileData.filepath)

  })
  // project.emptyDir(project.opentrans_orders)
  // project.XMLCheck(project.opentrans_orders)
}

/* -------------------------------------------------------------------------- */




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




/* -------------------------------------------------------------------------- */

function readFiles(dir, processFile) {
  // read directory
  fs.readdir(dir, (error, fileNames) => {
    if (error) throw error;
    fileNames.forEach(filename => {
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

/**
 * Running Command 
 * @param {} cmd 
 */
function runCmd(cmd) {
  console.log('Running Command: ', {cmd})
  var result = child_process.execSync(cmd);
  var result = result.toString('UTF8');
  return result;
}
module.exports = project
module.exports = files