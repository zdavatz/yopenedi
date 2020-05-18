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
var tags = []
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
                elements[1] = seg.match(/(\?.|[^:])+/g)
            } else {
                elements[0].push(seg)
            }
        } else {
            return seg
        }
    })
    var segs = _.compact(segs);
    var lineData = _.flatten(elements)
    var grammar = _.find(Grammar, (o) => {
        return o.name == [key]
    })
    var grammar = grammar ? grammar : null;
    var matchedData = null;
    if (grammar && grammar.render && grammar.match) {
        var matchedData = matchData(lineData, grammar.match)
    }
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
// //
// var getElemetData = function (line) {
//     return line.match(/(\?.|[^:])+/g)
// }
/* -------------------------------------------------------------------------- */
parseEDI.parse = function (doc) {     
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
        return line
    });
    //
    _.each(tags, (tag, index) => {
        var prev = tag;
        var line = linesArr[index].substring(4, 100)
        var i = index;
        if (struc.length == 0 && tag) {
            struc.push("Root")        
        }
        // return;
        // IF is Parent
        // LIN: {START} CREATE PRODUCTS ARRAY HEADER
        // {START}
        // LIN: PRODUCTS CLOSE
        // if (tag !== "LIN" && (order[order.length - 1] == "LIN") && ((_.includes(start, tag)) || _.includes(start, tag))) {
        //     console.log("((((((((((((((((((((((((((((((((((((((((((",{
        //         tag: tag,
        //         lin: order[order.length - 1] == "LIN",
        //         isHeader: _.includes(start, tag),
        //         isClosing: _.includes(skip, tag),
        //     })
        //     out.push("</PRODUCTS>")
        // }
        // if(_.includes(skip, tag) && tag !== "LIN" && (order[order.length - 1] == "LIN")){
        //     console.log('SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS ')
        //     out.push("</PRODUCTS>")
        // }
        //
        if (_.includes(start, tag)) {
            // HEADERS ARRAY
            order.push(tag)
            /* -------------------------------------------------------------------------- */
                    // {END}
            /* -------------------------------------------------------------------------- */
            // PARSE HEADER
            if (struc[struc.length - 1] !== tag) {
                // console.log("New Tag", tag, struc[struc.length - 1])
                struc.push(tag)
                out.push('<' + tag + ' attr="' + line + '">')
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
                out.push('<' + tag + ' attr="' + line + '">' + line + "</" + tag + ">")
            }
            // out.push('<' + tag + ' attr="' + line + '">'+ renderXMLElement(tag,index,line) + "</" + tag + ">")
        }

        // console.log('last index: ',order)

        // if(tag == "LIN"){
            console.log("IS LIN+++++++++++++",tag , prev)
        // }
        // // Closing Tag 
        if (_.includes(skip, tag)) {
            out.push("</" + struc[struc.length - 1] + ">")
            console.log('CLOSING: ', "</" + struc[struc.length - 1] + ">")
            // console.log('struc',struc)
            console.log('************Closing: ', struc[struc.length - 1], tag, "</" + struc[struc.length - 1] + ">")
            console.log("Removing: ", struc, struc[struc.length - 1])
            struc.splice(struc.lastIndexOf(tag), 1);
            console.log("Removed: ", struc, tag)
         
        }
    })
    out.push("</" + order[0] + ">")
    // console.log(out)
    out.push('</edi>')
    var xml = out.join("")
    console.log({
        tags,
        struc,
        order
    })
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
// Render XML 
function renderXMLElement(index) {
    console.log(ediData[index].key)
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
    // console.log('+++++++++Rendered: ', line)
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
var xml = parseEDI.parse(doc)
// REPLACE XML TAGS
var xml = parseEDI.setTags(xml)
// console.log(ediData[3])
console.log(xml, tags)
writeFile(xml)
function writeFile(data) {
    fs.writeFile(meteorPrivate + './xml-01.xml', xml, 'utf8', function (err) {
        if (err) {
            return console.log(err);
        } else {
            console.log("Writing Mew Data Success")
        }
    });
}