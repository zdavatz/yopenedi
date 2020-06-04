/**
* 
*/



const fs = require('fs');
const path = require('path');
/* -------------------------------------------------------------------------- */
console.log('___init_IO___')

/* -------------------------------------------------------------------------- */


project = {}

project.path = process.env['METEOR_SHELL_DIR'] + '/../../../'
project.public = process.env['METEOR_SHELL_DIR'] + '/../../../public/';
project.private = meteorPath + '/private/'
project.edifact_orders = meteorPath + '/edifact_orders/'
project.opentrans_orders  = meteorPath + '/opentrans_orders/'



files = {}

files.readDir = function(dir){
    readFiles(dir, (filepath, name, ext, stat) => {
        console.log("________________________");
        console.log('file path:', filepath);

        var text = fs.readFileSync(filepath, 'utf8');
        // fs.writeFileSync('foo.txt', text, 'utf8');
        console.log(text)
        // console.log('file name:', name);
        // console.log('file extension:', ext);
        // console.log('file information:', stat);
        console.log("________________________");
      })
}

files.readDir(project.edifact_orders)

function readFiles(dir, processFile) {
    // read directory
    fs.readdir(dir, (error, fileNames) => {
      if (error) throw error;
  
      fileNames.forEach(filename => {

        const name = path.parse(filename).name;

        const ext = path.parse(filename).ext;

        const filepath = path.resolve(dir, filename);

        fs.stat(filepath, function(error, stat) {
          if (error) throw error;

          const isFile = stat.isFile();
  
          // exclude folders
          if (isFile) {
            // callback, do something with the file
            processFile(filepath, name, ext, stat);
          }
        });
      });
    });
  }

