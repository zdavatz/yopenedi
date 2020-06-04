/**
 * 
 */
const fs = require('fs');
const path = require('path');
import Parse from './parse.js'
/* -------------------------------------------------------------------------- */
console.log('___init_IO___')
/* -------------------------------------------------------------------------- */
project = {}
project.path = process.env['METEOR_SHELL_DIR'] + '/../../../'
project.public = process.env['METEOR_SHELL_DIR'] + '/../../../public/';
project.private = project.path + '/private/'
/* -------------------------------------------------------------------------- */
project.edifact_orders = project.path + '/edifact_orders/'
project.opentrans_orders = project.path + '/opentrans_orders/'
project.edifact_orders_done = project.path + 'edifact_orders_done/'
files = {}

/* -------------------------------------------------------------------------- */

// console.log(parseEdiDoc)
project.readDir = function (dir,func) {
  readFiles(dir, (fileData) => {
    var doc = fs.readFileSync(fileData.filepath, 'utf8');
    var xml = Parse.parseEdiDoc(doc)
    writeFile(project.opentrans_orders + fileData.name + '.xml', xml)
  })
}
/* -------------------------------------------------------------------------- */

function writeFile(file, data) {
  console.log('Writing file..........', file)
  fs.writeFile(file, data, 'utf8', function (err) {
    if (err) {
      return console.log(err);
    } else {
      console.log("Writing Mew File [Success]", file)
    }
  });
}
/* -------------------------------------------------------------------------- */

project.readDir(project.edifact_orders)


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
      fs.stat(fileData.filepath, function (error, stat) {
        if (error) throw error;
        var isFile = stat.isFile();
        // exclude folders
        if (isFile) {
          // callback, do something with the file
          processFile(fileData);
        }
      });
    });
  });
}
module.exports = project
module.exports = files