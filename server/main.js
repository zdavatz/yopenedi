import {
  Meteor
} from 'meteor/meteor';
import _ from 'lodash'
import './collections.js'
import './api.js'
import './cron.js'

import './parse.edi.js'
const settings = Meteor.settings;

/* -------------------------------------------------------------------------- */

project.processEdifactDir(project.edifact_orders)

if(settings.private.resetDB){
  console.log('Resetting DB......')
  Items.remove({})
  console.log('Resetting DB......{SUCCESS}')
}