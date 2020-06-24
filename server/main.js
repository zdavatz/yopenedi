import {
  Meteor
} from 'meteor/meteor';
import _ from 'lodash'



import './api.js'
// import './express.api.js'

import './email.js'

const settings = Meteor.settings;

if (settings && !settings.private.isTesting) {
  console.log('Running Cron..')
  import './cron.js'
}