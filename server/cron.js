import {
    Meteor
  } from 'meteor/meteor';
import getMail from './email.js'


/* -------------------------------------------------------------------------- */
// Check Messages every 5 mins
SyncedCron.add({
  name: 'Checking Messages Cron......',
  schedule: function(parser) {
    return parser.text('every 30 seconds');
    // return parser.text('every 5 minutes');
  },
  job: function() {
    console.error('--------------Checking Messages Cron-------------------')
    getMail()
    console.error('=================================')
     
  }
});



/* -------------------------------------------------------------------------- */


SyncedCron.start();