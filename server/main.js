import {
  Meteor
} from 'meteor/meteor';
import _ from 'lodash'

// import './email.js'
// import './io.js'


//
var apiMessage = "<Status><Code>200</Code><Description>OK</Description><Text>Successful: Saved into 'YWE_2020-06-14-13.39.43.487.xml'</Text></Status>"

var re = new RegExp("<Code>(.+?)<\/Code>", "g");
var re = /<Code>([^<]+)<\/Code>/ig
var re = new RegExp("[^(<Code>)](?:.*(\r?\n?).*)*(?=\<\/Code\>)","gi")
var remove = /(<([^>]+)>)/ig

var myArray = apiMessage.match(re);
// var r = myArray[0].replace(remove, " ")

console.log(myArray)
return

/* -------------------------------------------------------------------------- */
// FOR TESTING without CRON
App.checkMessages()
// Meteor.seTimeout(function(){},1000)
// project.processEdifactDir(project.edifact_orders)

// project.XMLCheck(project.opentrans_orders)
/* -------------------------------------------------------------------------- */

/* -------------------------------------------------------------------------- */
// Check Messages every 5 mins
SyncedCron.add({
  name: 'Checking Messages Cron......',
  schedule: function(parser) {
    // return parser.text('every 30 seconds');
    return parser.text('every 5 minutes');
  },
  job: function() {
    console.log('Checking Messages Cron...')
    App.checkMessages()    
  }
});


/* -------------------------------------------------------------------------- */


SyncedCron.add({
  name: 'Convert Edifact files to OpenTrans',
  schedule: function(parser) {

    return parser.text('every 7 minutes');
  },
  job: function() {
    console.log('Checking Messages Cron...') 
    project.processEdifactDir(project.edifact_orders)
  }
});

/* -------------------------------------------------------------------------- */


SyncedCron.start();