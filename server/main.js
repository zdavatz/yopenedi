import {
  Meteor
} from 'meteor/meteor';
import _ from 'lodash'

import './email.js'
import './io.js'

import './api.js'





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