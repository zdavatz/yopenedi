const express = require('express');
const fileUpload = require('express-fileupload');
const cors = require('cors');
const bodyParser = require('body-parser');
const morgan = require('morgan');
const _ = require('lodash');
var child_process = require('child_process');
const fs = require('fs');
const fse = require('fs-extra')
const path = require('path');


import './io.js'




const settings = Meteor.settings;

console.log({settings})

const app = express();

// enable files upload
app.use(fileUpload({
    createParentPath: true
}));

//add other middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: true}));
app.use(morgan('dev'));

//start app 
var port = process.env.PORT || 4000;
var port = 4000;

app.post('/as2', async (req, res) => {
    try {
        if(!req.files) {
            res.send({
                status: false,
                message: 'No file uploaded'
            });
        } else {
            console.log('File is uploaded',req.files.file)
            var file = req.files.file;

            var ext = path.extname(file.name)
            console.log({ext})
            var outputPath =  project.edifact_orders_encryped + file.name           
            fs.writeFileSync(outputPath, file.data, "binary", (err, result) => {
                if (err) {
                  console.log(err);
                }
            });

            // var pkey = '/etc/letsencrypt/live/test.yopenedi.ch/privkey.pem'
            // var pkey = '/home/neox/caKey.pem'
            // var pkey = settings.private.private_key
            // console.log({pkey})
            // var dec = 'openssl cms -decrypt -binary -in ' + outputPath + '-inform DER -out read -inkey '+ pkey;
            // console.log({dec})
            // runCommand(dec)


            res.send({
                status: true,
                message: 'File is uploaded',
                data: {
                    name: file.name,
                    mimetype: file.mimetype,
                    size: file.size
                }
            });
        }
    } catch (err) {
        res.status(500).send({err});
    }
});


function runCommand(cmd) {
    console.log('Running Command (run.external): ', {
      cmd
    })
    var result = child_process.execSync(cmd);
    var result = result.toString('UTF8');
    return result;
} 


app.listen(port, () => 
  console.log(`App is listening on port ${port}.`)
);