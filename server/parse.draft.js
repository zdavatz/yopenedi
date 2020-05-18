const _ = require('lodash')
var xml = require('xml');
var builder = require('xmlbuilder');
/* -------------------------------------------------------------------------- */
var doc = Assets.getText('noname')
var keys = [];
var tags = []
var linesArr = []
var segments = [];
var messages = []
var output = {}
/* -------------------------------------------------------------------------- */
// var elem = xml.element({ _attr: { decade: '80s', locale: 'US'} });
// var stream = xml({ toys: elem }, { stream: true });
// stream.on('data', function (chunk) { 
//     console.log(chunk)
//     console.log('Written XML')
// });
// elem.push({ toy: 'Transformers' });
// elem.push({ toy: 'GI Joe' });
// elem.push({ toy: [{name:'He-man'}] });
/* -------------------------------------------------------------------------- */
var EdiParse = function (string) {
    this.string = string;
}
EdiParse.prototype.conv = function (doc) {
    var root = builder.create('Edi');
    var lines = doc.split(/['\n\r]+/);
    lines = lines.map(function (line) {
        if (line) {
            var key = line.substring(0, 3)
            keys.push({
                [key]: line
            })
            tags.push(key)
            linesArr.push(line)

        }
    });
    /* -------------------------------------------------------------------------- */
    var xml = root.end({
        pretty: true
    });
    var root = builder.create('Edi');
    /* -------------------------------------------------------------------------- */
    var start = ["UNB", "UNG", "UNH", "LIN"]
    var skip = ["UNT", "UNE", "UNZ"]
    var parent, prev;
    var struc = []
    
    var out = []
    var order = []
    var obj = {}
    out.push('<?xml version="1.0" encoding="utf-8" standalone="yes"?><edi>')
    _.each(tags, (tag, index) => {
        if (struc.length == 0 && tag) {
            struc.push("Root")
        }
        // IF is Parent
        if (_.includes(start, tag)) {
            order.push(tag)
            // INCLUDES HEADER
            if (struc[struc.length - 1] !== tag) {
                console.log("New Tag", tag, struc[struc.length - 1])
                struc.push(tag)
                out.push("<" + tag + ">")
            } else if (struc[struc.length - 1] === tag) {
                out.push("</" + tag + ">")
                out.push("<" + tag + ">")
            }
            var parent = struc[struc.length - 2]
            var k = struc[struc.length - 1]
            console.log('Tag: ', tag, 'Parent: ', parent, k)
        } else {
            // Close the tags in loop
            var data = linesArr[index].substring(4, 100)
            console.log(data)
            out.push("<" + tag + ">" + data + "</" + tag + ">")
        }
        // // Closing Tag 
        if (_.includes(skip, tag)) {
            out.push("</" + struc[struc.length - 1] + ">")
            console.log('Closing: ', struc[struc.length - 1], tag, "</" + struc[struc.length - 1] + ">")
            struc.splice(struc.lastIndexOf(tag), 1);
        }
    })


    out.push("</" + order[0] + ">")
    // console.log(out)
    out.push('</edi>')
    var x = out.join("")
    console.log(x)
    console.log(tags,struc)
}

EdiParse.prototype.conv(doc)
function getSegment(str) {
    return str.match(/(\?.|[^\+])+/g)
}
function getElement(segArr) {
    var g = segArr.map(function (segment) {
        return segment.match(/(\?.|[^:])+/g)
    })
}
function extractBlock(start, end) {}
function buildElem(parent, child) {}
// var UN = root.ele(tag)
// UN.att("name","is something")
// UN.ele("A","WWW")
// // console.log(obj[tag])


// Draft
// var root = builder.create('squares');
// root.ele('data',"about everything");
// root.com('f(x) = x^2');
// for(var i = 1; i <= 5; i++)
// {
//   var item = root.ele('data',"about everything");
//   item.att('x', i);
//   item.att('y', i * i);
//   item.ele("hamza","is here")
// }
// var xml = root.end({ pretty: true});
// console.log(xml);