const fs = require('fs');
const path = require('path');
const _ = require('lodash');
import './grammar.js'
/* -------------------------------------------------------------------------- */
meteorPath = process.env['METEOR_SHELL_DIR'] + '/../../../'
publicPath = process.env['METEOR_SHELL_DIR'] + '/../../../public/';
meteorPrivate = meteorPath + '/private/'
meteorPrivate = meteorPath + '/exported/'
/* -------------------------------------------------------------------------- */
var doc = Assets.getText('noname')
/* -------------------------------------------------------------------------- */
var keys = [];
var tags = [];
var headkeys = [];
var ediData = [];
var linesArr = []
/* -------------------------------------------------------------------------- */
var start = ["UNB", "UNG", "UNH", "LIN"]
var skip = ["UNT", "UNE", "UNZ"]
var struc = []
var out = []
var order = []
var parent, prev;
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
out.push('<?xml version="1.0" encoding="utf-8" standalone="yes"?><edi>')
/* -------------------------------------------------------------------------- */
var parseEDI = {}
parseEDI.regex = {
    line: /['\n\r]+/,
    segment: /(\?.|[^\+])+/g,
    element: /(\?.|[^\+])/g,
    component: /(\?.|[^:])+/g
}
/* -------------------------------------------------------------------------- */
var getSegment = function (line) {
    var segs = line.match(parseEDI.regex.segment)
    var key = line.substring(0, 3)
    var elements = [
        [],
        []
    ]
    var segs = segs.map((seg) => {
        if (seg !== key) {
            if (seg.indexOf(":") > -1) {
                // elements[1] = seg.match(/(\?.|[^:])+/g)
                elements[1] = seg.split(':');
                // console.log(elements[1]);
                // console.log(seg.split(':'));
            } else {
                elements[0].push(seg)
            }
        } else {
            elements[0].push("")
        }
    })
    var segs = _.compact(segs);
    var lineData = _.flatten(elements)
    var lineData = elements[1]
    var grammar = _.find(Grammar, (o) => {
        return o.name == [key]
    })
    var grammar = grammar ? grammar : null;
    var matchedData = null;
    if (grammar && grammar.render && grammar.match) {
        var matchedData = matchData(lineData, grammar.match)
    }
    console.log('----------')
    console.log(key, ': ', line ,": ",elements)
    // dataElements => Contains Elements that seperated by ":"
    // Line Data => Contains Comp+ DataSlements
    var out = {
        key,
        line,
        segs,
        elements,
        lineData,
        matchedData
    }
    var out = _.assign(out, grammar)
    return out;
}
/* -------------------------------------------------------------------------- */
// GENERATE HEAD {PARENT KEYS}
function setHeaderKeyArr(tagArr) {
    _.each(tagArr, (tag, index) => {
        if (_.includes(start, tag)) {
            headkeys.push(tag)
        }
    })
}
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
// //
// var getElemetData = function (line) {
//     return line.match(/(\?.|[^:])+/g)
// }
var outJSON = {};
var heads = []
/* -------------------------------------------------------------------------- */
// SETTING DOC KEYS PER LINES
function setKeys(doc) {
    var lines = doc.split(/['\n\r]+/);
    lines = lines.map(function (line) {
        if (line) {
            var key = line.substring(0, 3)
            keys.push({
                [key]: line
            })
            tags.push(key)
            linesArr.push(line)
            var segs = getSegment(line)
            ediData.push(segs);
            // console.log('----SUCCESS---- Segment', segs)
        }
        // return line
    });
    //
}
/* -------------------------------------------------------------------------- */
var grammarKeys = [];
function generateGrammerTags(tags){
    var tags = _.uniq(tags);
    _.each(tags, (tag, index) => {
        grammarKeys.push({name:tag,tag: "", render: "", match: "", parent: "", children: "", isHeader: ""})
    })
}
/* -------------------------------------------------------------------------- */
var start = ["UNB", "UNG", "UNH", "LIN"]
var skip = ["UNT", "UNE", "UNZ"]
var struc = []

function renderXML(){
    _.each(tags, (tag, index) => {
        var prev = tag;
        var line = linesArr[index].substring(4, 100)
        var i = index;
        if (struc.length == 0 && tag) {
            struc.push("Root")
        }
        // LIN: {START} CREATE PRODUCTS ARRAY HEADER
        if (tag == "LIN" && (order[order.length - 1] !== "LIN")) {
            out.push("<PRODUCTS>")
        }
        if (_.includes(start, tag)) {
            // HEADERS ARRAY
            order.push(tag)
            // PARSE HEADER
            if (struc[struc.length - 1] !== tag) {
                // console.log("New Tag", tag, struc[struc.length - 1])
                struc.push(tag)
                heads.push(tag)
                //attr="' + line + '"
                out.push('<' + tag + '>')
                // NEXT IF NOT THE SAME AS THE CURRENT TAG (LIN)
            } else if (struc[struc.length - 1] === tag) {
                // used to close the tag
                out.push("</" + tag + ">")
                out.push("<" + tag + ">")
            }
            var parent = struc[struc.length - 2]
            var k = struc[struc.length - 1]
            // console.log('Tag: ', tag, 'Parent: ', parent, k)
        } else {
            if (ediData[index].matchedData) {
                // console.log('____________ HAS DATA INDEX ___________')
                var x = renderXMLElement(index)
                out.push(x)
            } else {
                //  attr="' + line + '"
                out.push('<' + tag +'>' + line + "</" + tag + ">")
            }
            // out.push('<' + tag + ' attr="' + line + '">'+ renderXMLElement(tag,index,line) + "</" + tag + ">")
        }
        // // Closing Tag 
        if (_.includes(skip, tag)) {
            out.push("</" + struc[struc.length - 1] + ">")
            if (struc[struc.length - 1] == "LIN") {
                out.push("</PRODUCTS>")
            }
            // console.log('CLOSING: ', "</" + struc[struc.length - 1] + ">")
            // console.log('************Closing: ', struc[struc.length - 1], tag, "</" + struc[struc.length - 1] + ">")
            // console.log("Removing: ", struc, struc[struc.length - 1])
            struc.splice(struc.lastIndexOf(tag), 1);
            // console.log("Removed: ", struc, tag)
        }
    })
    out.push("</" + order[0] + ">")
    out.push('</edi>')
    var xml = out.join("")
    return xml;
}
/* -------------------------------------------------------------------------- */
function renderJSON(){
    _.each(tags, (tag, index) => {
        var prev = tag;
        var line = linesArr[index].substring(4, 100)
        var i = index;
        if (struc.length == 0 && tag) {
            struc.push("Root")
        }
        if (tag == "LIN" && (order[order.length - 1] !== "LIN")) {
            out.push("<PRODUCTS>")
        }
        if (_.includes(start, tag)) {
            // HEADERS ARRAY
            order.push(tag)
            // PARSE HEADER
            if (struc[struc.length - 1] !== tag) {
                // console.log("New Tag", tag, struc[struc.length - 1])
                struc.push(tag)
                heads.push(tag)
                //attr="' + line + '"
                out.push('<' + tag + '>')
                // NEXT IF NOT THE SAME AS THE CURRENT TAG (LIN)
            } else if (struc[struc.length - 1] === tag) {
                // used to close the tag
                out.push("</" + tag + ">")
                out.push("<" + tag + ">")
            }
            var parent = struc[struc.length - 2]
            var k = struc[struc.length - 1]
            console.log('Tag: ', tag, 'Parent: ', parent, k)
        } else {
            if (ediData[index].matchedData) {
                // console.log('____________ HAS DATA INDEX ___________')
                var x = renderXMLElement(index)
                out.push(x)
            } else {
                //  attr="' + line + '"
                out.push('<' + tag +'>' + line + "</" + tag + ">")
            }
            // out.push('<' + tag + ' attr="' + line + '">'+ renderXMLElement(tag,index,line) + "</" + tag + ">")
        }
        // console.log('last index: ',order)
        // if(tag == "LIN"){
        console.log("IS LIN+++++++++++++", tag, prev)
        // }
        // // Closing Tag 
        if (_.includes(skip, tag)) {
            out.push("</" + struc[struc.length - 1] + ">")
            if (struc[struc.length - 1] == "LIN") {
                out.push("</PRODUCTS>")
            }
            console.log('CLOSING: ', "</" + struc[struc.length - 1] + ">")
            // console.log('struc',struc)
            console.log('************Closing: ', struc[struc.length - 1], tag, "</" + struc[struc.length - 1] + ">")
            console.log("Removing: ", struc, struc[struc.length - 1])
            struc.splice(struc.lastIndexOf(tag), 1);
            console.log("Removed: ", struc, tag)
        }
    })
    out.push("</" + order[0] + ">")
    out.push('</edi>')
    var xml = out.join("")
    return xml;
}
/* -------------------------------------------------------------------------- */
// REPLACE HTML TAGS
parseEDI.setTags = function (xml) {
    for (i = 0; i < Grammar.length; i++) {
        var tag = Grammar[i].name
        let re = new RegExp(`\\b${tag}\\b`, 'g');
        var xml = xml.replace(re, Grammar[i].tag);
    }
    return xml
}
/* -------------------------------------------------------------------------- */
// Render XML //
function renderXMLElement(index) {
    // console.log('renderXMLElement',ediData[index].key)
    var i = index;
    if (!ediData[index]) {
        console.log('SKIPPED')
        return
    }
    var line = ediData[i].render
    var data = ediData[i].matchedData
    if (!line || !data) {
        console.log('SKIPPED', 'NO DATA or Renderer Text')
        return
    }
    for (i = 0; i < data.length; i++) {
        var key = _.keys(data[i]);
        var key = key[0]
        var line = line.replace(key, data[i][key])
    }
    return line
}
/* -------------------------------------------------------------------------- */
// Match two arrays with Data and Grammer
// matchData(["CODE", 120, "PCS"],["$code","$qty","$unit"])
function matchData(arr, grammar) {
    var arr = arr.map((arrEle, index) => {
        return {
            [grammar[index]]: arrEle
        }
    })
    return arr;
}
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
setKeys(doc)
var xml = renderXML(doc)
// REPLACE XML TAGS
var xml = parseEDI.setTags(xml)
// console.log(ediData[3])
// console.log(xml, tags)
setHeaderKeyArr(tags)
generateGrammerTags(tags)
// console.log('headkeys',{headkeys,order,struc})
// console.log({
//     keys,
//     tags,
//     struc,
//     order,
//     heads,
//     outJSON,
// })
// Generated Grammar keys
// console.log(JSON.stringify(grammarKeys))
writeFile(xml)
// function addParentTag(parent,children){
//     children = []
//     _.each(tags,(tag,index)=>{
//         if(tag=="LIN"){
//             console.log(tag,index)
//             children.push({[tag]:index})
//             // if(_.includes(start,struc[struc.length - 1])){
//                 console.log(struc[struc.length - 1])
//             // }
//         }
//     })
//     console.log(children)
//     console.log(JSON.stringify(tags))
// }
// addParentTag()
/* -------------------------------------------------------------------------- */
function writeFile(data) {
    fs.writeFile(meteorPrivate + './xml-01.xml', xml, 'utf8', function (err) {
        if (err) {
            return console.log(err);
        } else {
            console.log("Writing Mew Data Success")
        }
    });
}