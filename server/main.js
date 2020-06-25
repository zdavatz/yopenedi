import {
  Meteor
} from 'meteor/meteor';
import _ from 'lodash'
import './collections.js'
import './api.js'
import './email.js'
const settings = Meteor.settings;
if (settings && !settings.private.isTesting) {
  console.log('Running Cron..')
  import './cron.js'
}
if(settings.private.resetDB){
  console.log('Resetting DB......')
  Items.remove({})
  console.log('Resetting DB......{SUCCESS}')
}