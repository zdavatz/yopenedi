import {
  Meteor
} from 'meteor/meteor';
import getMail from './email.js'


const settings = Meteor.settings;
/* -------------------------------------------------------------------------- */

if (settings.private.checkEmail) {
  console.log('Email Check is disabled....')
  console.log('Check Messages every 5 mins...')
  SyncedCron.add({
    name: 'Checking Messages Cron......',
    schedule: function (parser) {
      // return parser.text('every 30 seconds');
      return parser.text('every 5 minutes');
    },
    job: function () {
      console.error('--------------Checking Messages Cron-------------------')
      getMail()
      console.error('=================================')

    }
  });
} else {
  console.log('Email Check is disabled....')
}



/* -------------------------------------------------------------------------- */


SyncedCron.start();