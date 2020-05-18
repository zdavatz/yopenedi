import {
  Meteor
} from 'meteor/meteor';
// const EDI = require('edi');
import EDI from './edi'
const _ = require('lodash');

const {
  toXML
} = require('jstoxml');


import './parse.js'

return


// import './email.js'

//https://www.npmjs.com/package/edifact-parser

const EDIS = require("edifact-parser");

var edif = Assets.getText('noname')

// https://www.stylusstudio.com/edifact/D96A/DIRDEF.htm


const elementScheme = {
  messages: ["mid", "mtype", "msubtype", "oref"],
  UNB: ["idFrom","idTo"],
  BGM: ["orderCode", "orderCode", "orderRef"],
  DTM: ["dateCode","dateType", "deliveryTime","deliveryTimeEarliest", "deliveryTimeLatest","deliveryDaysRange", "deliveryTimeRange"],
  RFF: ["mrefCode","mrefType","mref","refType","ref","refVAT","refs"],
  NAD: ["nameAddressesType","naBuyer", "naSupplier","naDeliveryParty","naMessageReceiver","naInvoicee","naInvoiceeAddress1","naInvoiceeAddress2","naInvoiceeAddressCity","naInvoiceeAddressPostalCode","naInvoiceeAddressCountry","nameAdresses"],
  CUH: ["currency"],
  UNS: ["summarySeparator"],
  CNT: ["controlTotalType", "controlTotal" ],
  UNT: ["segmentQty"],
  UNZ: ["documentEnd"],
  LIN: ["productLines"],
  PIA: ["productsType","productsID", "itemsType"],
  QTY: ["productsQtyType","productsQtyType"],
  PRI: ["productsPriceType","productsPriceTypeCode","productsPriceTypeCode","productsPriceTypeCode"],
}


// import?

/* -------------------------------------------------------------------------- */

const xmlOptions = {
  header: '<?xml version="1.0" encoding="utf-8" standalone="yes"?>',
  indent: '  '
};
var output = {
  ORDER: {
    _attrs: {
      type: 'standard',
      xmlns: "http://www.opentrans.org/XMLSchema/2.1",
      "xmlns:bmecat": "http://www.bmecat.org/bmecat/2005",
      version: "2.1"
    },
    ORDER_HEADER: {
      CONTROL_INFO: {
        GENERATION_DATE: "2020-01-22T07:35:18.6258"
      },
      ORDER_INFO: {
        ORDER_ID: "PLEX-141269",
        ORDER_DATE: "2020-01-22",
        DELIVERY_DATE: {
          _attrs: {
            type: "optional",
          },
          DELIVERY_START_DATE: "",
          DELIVERY_END_DATE: ""
        },
        // 
        _content: {
          _name: 'bmecat:LANGUAGE',
          _content: "fra",
          _attrs: {
            default: 'true'
          }
        }



      },
      

    },
    Messages: [],
    Segments: []
  }
}




// console.log('Output XML:',toXML(output, xmlOptions))

/* -------------------------------------------------------------------------- */


// return

//
//

// 



// var msg = new EDI(edif);
// var batches = msg.bsegments();
// var batch = batches[0];
// // console.log(msg.lines())

// console.log('/* Segments */',batch)
// // console.log(msg.bsegments())
// console.log('First batch in EDI message sent at ' + batch.bisotime() + ' from ' +  batch.bfrom() + ' to ' + batch.bto() + '.');

// // 

// return

/* -------------------------------------------------------------------------- */

const doc = new EDIS(edif);
const segments = doc.bsegments();
const messages = doc.msegments();
var msg = messages[1];
const sbatch = segments[0];


// segment


function getMessages(messages,type) {
  console.log('Type', type)

  _.each(messages, (msg) => {
    
    if(type == 'msg'){
      console.log('Message:')
      var s = {}
      s.mid = msg.mid(); 
      s.mtype = msg.mtype(); 
      s.msubtype = msg.msubtype(); 
      s.oref = msg.oref();
      s.mproduct = msg.mproduct(); 
      console.log(s)
      output.ORDER.Messages.push(s)
    }

    if(type == 'sgm'){
      console.log('Segment:')
      var d = {
        from: msg["idFrom"](),
        to: msg.idTo(),
        // BGM
        orderCode: msg.orderCode(),
        orderType: msg.orderType(),
        orderRef: msg.orderRef(),
        // DTM
        dateCode: msg.dateCode(),
        dateCode: msg.dateCode(),
        // deliveryTime: sbatch.deliveryTime(),
        deliveryTimeEarliest: msg.deliveryTimeEarliest(),
        deliveryTimeLatest: msg.deliveryTimeLatest(),
        deliveryDaysRange: msg.deliveryDaysRange()?msg.deliveryDaysRange():null,
        deliveryTimeRange: msg.deliveryTimeRange()?msg.deliveryTimeRange():null,
      }
      
      // console.log(mbatch.mid)
      console.log(d)
      output.ORDER.Segments.push(d)
    }

    

    // console.log(msg.mid(), msg.mtype(), msg.msubtype(), msg.mproduct())
  })

  // console.log(JSON.stringify(output,null,1))
}


getMessages(messages, "msg")

getMessages(segments, "sgm")


function createMsg(data){

}


function createSegment(data){

}