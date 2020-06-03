/**
 * 
 # Skipped TAGS
    - RFF
*/
const fs = require('fs');
const path = require('path');
const _ = require('lodash');
const format = require('xml-formatter');
import './grammar.js'
/* -------------------------------------------------------------------------- */
meteorPath = process.env['METEOR_SHELL_DIR'] + '/../../../'
publicPath = process.env['METEOR_SHELL_DIR'] + '/../../../public/';
meteorPrivate = meteorPath + '/private/'
meteorPrivate = meteorPath + '/exported/'
/* -------------------------------------------------------------------------- */
var fileName = '3_order_sample_from_REXEL_utf-8'
var doc = Assets.getText(fileName)
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
var structuredData;
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
out.push('<?xml version="1.0" encoding="utf-8" standalone="yes"?>')
out.push(`
<ORDER type="standard" xmlns="http://www.opentrans.org/XMLSchema/2.1" xmlns:bmecat="http://www.bmecat.org/bmecat/2005" version="2.1">
<ORDER_HEADER>
<CONTROL_INFO>
    <GENERATOR_INFO>yopenedi</GENERATOR_INFO>
    <GENERATION_DATE>` + new Date() + `</GENERATION_DATE>
</CONTROL_INFO>
</ORDER_HEADER>
`)
/* -------------------------------------------------------------------------- */
parseEDI = {}
parseEDI.regex = {
    line: /['\n\r]+/,
    segment: /(\?.|[^\+])+/g,
    element: /(\?.|[^\+])/g,
    component: /(\?.|[^:])+/g
}
/* -------------------------------------------------------------------------- */
setKeys(doc)
/* -------------------------------------------------------------------------- */
function getSegment(line) {
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
    ////
    var segs = _.compact(segs);
    var lineData = _.flatten(elements)
    var lineData = elements[1]
    var grammar = _.find(Grammar, (o) => {
        return o.name == [key]
    })
    var grammar = grammar ? grammar : null;
    var matchedData = null;
    //
    if (grammar && grammar.render && grammar.match) {
        // var matchedData = matchData(lineData, grammar.match)
        console.error('No Matched Data Grammar keys')
        var matchedData = matchDataBlock(elements, grammar.match)
    }
    var testKeys = ['NAD', 'BGM', "DTM", "FTX", "RFF", "QTY", "CTA", "COM", "LIN", "NAD", "IMD", "PRI", "PIA", "BGM", "UNZ"]
    if (key == testKeys[0] || key == "CTA") {
        // console.log('----------', key, '------------')
        // console.log('GetSegment: Start: ', elements, ": ", grammar.match)
        // console.log('GetSegment: Output: ', matchedData)
        // console.log('=========')
    }
    //
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
// Generates The Grammer Tags for all current Documet Auto !== Manual
// USED TO SET GRAMMAR ARRAY VALUE
var grammarKeys = [];
function generateGrammerTags(tags) {
    var tags = _.uniq(tags);
    _.each(tags, (tag, index) => {
        grammarKeys.push({
            name: tag,
            tag: "",
            render: "",
            match: "",
            parent: "",
            children: "",
            isHeader: ""
        })
    })
}
/* -------------------------------------------------------------------------- */
function getGrammar(key, object) {
    var grammar = _.find(Grammar, (o) => {
        return o.name == [key]
    })
    if (grammar && grammar[object]) {
        // console.log('getGrammar: ', grammar[object])
        return grammar[object];
    }
}
// getGrammar("BGM", "tag")
/* -------------------------------------------------------------------------- */
function getRenderedData(index) {
    if (!index || !ediData[index]) {
        throw new Meteor.Error('findRenderedData', 'Index Error')
    }
    // console.log("+++++++++++++++++++++++++", ediData[index])
    return ediData[index]
}
/* -------------------------------------------------------------------------- */
function generateStructuredArr() {
    var structuredArr = []
    strucJSON = [] //
    var orderJSON = []
    var parent = ["UNB", "UNG", "UNH", "LIN", "NAD", "CUX"]
    var start = ["UNB", "UNG", "UNH", "LIN"]
    var skip = ["UNT", "UNE", "UNZ"]
    var looped = ["LIN", "NAD"] //
    _.each(tags, (tag, index) => {
        var prev = tag;
        var line = linesArr[index].substring(4, 100)
        var i = index;
        // console.log(i, tag, line)
        /* -------------------------------------------------------------------------- */
        // DATA
        // TAG CONTROLS 
        var element = ediData[index]
        if (element) {
            var elementsAll = ediData[index].elements
            // var line = ediData[index].render
            var data = ediData[index].matchedData
            // var rendered = getXMLElement(index) ? getXMLElement(index) : ediData[index].render
            // var key = elementsAll['key']
            // if has children 
        }
        /* -------------------------------------------------------------------------- */
        // SETTING UP PARENT STRUCTURE
        if (_.includes(start, tag)) {
            if (strucJSON.length == 0 && tag) {
                strucJSON.push("Root")
            }
            if (strucJSON[strucJSON.length - 1] !== tag) {
                // console.log("New Tag", tag, strucJSON[strucJSON.length - 1])
                console.log('PARENT: ', strucJSON[strucJSON.length - 1], '> ', tag)
                strucJSON.push(tag)
            } else if (strucJSON[strucJSON.length - 1] === tag) {
                // console.log("LOOP: =======================", tag, strucJSON[strucJSON.length - 1])
            }
        }
        /* -------------------------------------------------------------------------- */
        // PUSHING THE DATA.....
        // SKIP CLOSING TAG
        if (_.includes(skip, tag)) {
            // console.log('SKIP START', tag, strucJSON)
            strucJSON.splice(strucJSON.lastIndexOf(tag), 1);
            // console.log('SKIP END RM', tag, strucJSON)
            orderJSON.push(tag)
            // Adding Skipp Tags 
            structuredArr.push({
                tag: tag,
                line: line,
                close: strucJSON[strucJSON.length - 1],
                skip: true,
                index: index,
                // rendered: rendered,
                data: data
            })
            return
        }
        // PARENT -> CHILDREN // 
        if (_.includes(parent, tag)) {
            // console.log('MATCHED', tag)
            orderJSON.push(tag)
            structuredArr.push({
                tag: tag,
                line: line,
                index: index,
                children: [],
                parent: strucJSON[strucJSON.length - 2],
                // rendered: rendered,
                data: data,
                isParent: true
            })
        } else {
            var parentTag = structuredArr[structuredArr.length - 1]
            if (!parentTag) {
                return
            }
            // console.log({parentTag}, parentTag.children)
            parentTag.children.push({
                tag: tag,
                line: line,
                index: index,
                parent: structuredArr[structuredArr.length - 1].tag,
                data: data,
                // rendered: rendered
            })
        }
    })
    // Inject enclose tags looped
    var arr = setEnclosedTags(structuredArr, "NAD", "PARTIES")
    var arr = setEnclosedTags(arr, "LIN", "PRODUCTS")
    var arr = generatePriceLineAmount(arr)
    // var arr = setTagBeforeAfter(arr, "ORDER_HEADER", "BGM", "PRODUCTS")
    // console.log({
    //     strucJSON,
    //     orderJSON,
    //     // structuredArr
    // }, structuredArr)
    return arr;
}
/* -------------------------------------------------------------------------- */
var jsonReady = generateStructuredArr()
// // console.log(JSON.stringify(jsonReady))
var newXML = jsonToXML(jsonReady)
writeFile('json.export.json', JSON.stringify(jsonReady))
writeFile(fileName + 'xml.export.xml', newXML)
/* -------------------------------------------------------------------------- */
// var arr = ["UNH", "UNH", "NAD", "NAD", "NAD", "CUX", "LIN", "LIN", "LIN", "UNH"]
// setEnclosedTags(arr, 'NAD', 'Pareties')
// setEnclosedTags(arr, 'LIN', 'PRODUCTS')
function setEnclosedTags(arr, tag, enclosed) {
    var newArr = []
    _.each(arr, (el, index) => {
        if (el.tag == tag) {
            console.log('______ HEAD', tag)
            if (arr[index - 1]["tag"] !== tag) {
                newArr.push({
                    tag: enclosed
                })
            }
            newArr.push(el)
            if (arr[index + 1]["tag"] !== tag) {
                newArr.push({
                    tag: enclosed,
                    close: enclosed
                })
            }
        } else {
            newArr.push(el)
        }
    })
    return newArr;
}
/* -------------------------------------------------------------------------- */
var arr = ["UNH", "UNH", "NAD", "NAD", "NAD", "CUX", "LIN", "LIN", "LIN", "UNH"]
// setTagBeforeAfter(arr, "Batats", "BGM", "PRODUCTS")
function setTagBeforeAfter(arr, tag, start, close) {
    console.log('setTagBeforeAfter', {
        arr,
        tag,
        start,
        close
    })
    var newArr = []
    _.each(arr, (el, index) => {
        if (el.tag == start.tag) {
            newArr.push({
                tag: tag
            })
            newArr.push(el)
        } else if (el.tag == close.tag) {
            newArr.push(el)
            newArr.push({
                tag: tag,
                close: enclosed
            })
        } else {
            newArr.push(el)
        }
    })
    return newArr;
}
/* -------------------------------------------------------------------------- */
// check LENGTH 
// STRUCJSON //
// PARENT KEY
//  GENERATE PRICE {PRICE_ITEM}
function generatePriceLineAmount(arr) {
    _.each(arr, (jsonElem, index) => {
        // console.log(jsonElem)
        var price, qty, priceLinePrice;
        if (jsonElem.tag == "LIN") {
            _.each(jsonElem.children, (child) => {
                if (child.tag == "QTY") {
                    var data = ediData[child.index].matchedData;
                    var find = _.find(data, (o) => {
                        if (o["$qty"]) {
                            return o["$qty"]
                        }
                    })
                    qty = find["$qty"]
                }
                if (child.tag == "PRI") {
                    var data = ediData[child.index].matchedData;
                    var find = _.find(data, (o) => {
                        if (o["$PRICE"]) {
                            return o["$PRICE"]
                        }
                    })
                    price = find['$PRICE']
                    // console.log({price})
                }
            })
            if(qty && price){
                priceLinePrice = qty * price;
                priceLinePrice = priceLinePrice.toFixed(2)
                var newTag = {
                    name: "PRICE_LINE_AMOUNT",
                    tag: "PRICE_LINE_AMOUNT",
                    data: [[priceLinePrice],[]],
                    render: '<PRICE_LINE_AMOUNT>'+ priceLinePrice +'</PRICE_LINE_AMOUNT>',
                    isRendered: true
                }
                arr[index].children.push(newTag)
                console.log('_______________________D', {
                    qty, price, priceLinePrice,newTag
                })
            }
            // console.log(jsonElem.children)
        }
    })
    return arr
}
/* -------------------------------------------------------------------------- */
function generateSummary() {
    // PRICE_LINE_AMOUNT
}
function jsonToXML(jsonArr) {
    console.log('------------------')
    var keepOpen = ["UNB", "UNG", "UNH"]
    var start = ["UNB", "UNG", "UNH"]
    var skip = ["UNT", "UNE", "UNZ"]
    var xml = []
    var struc = []
    xml.push('<?xml version="1.0" encoding="utf-8" standalone="yes"?>')
    xml.push('<ORDER type="standard" xmlns="http://www.opentrans.org/XMLSchema/2.1" xmlns:bmecat="http://www.bmecat.org/bmecat/2005" version="2.1">')
    // ORDER_HEADER
    xml.push('<ORDER_HEADER>')
    xml.push(`<CONTROL_INFO>
    <GENERATOR>Yopenedi</GENERATOR>
    <GENERATION_DATE>` + new Date() + `</GENERATION_DATE>
    </CONTROL_INFO>`)
    //
    // ORDER_INFO
    xml.push('<ORDER_INFO>')
    _.each(jsonArr, (jsonElem) => {
        if(jsonElem.skip){
            console.log('__________________________ SKIPPED TAG')
        }
        var tag = jsonElem.tag;
        console.log('Processing:', tag)
        if (struc.length == 0 && tag) {
            struc.push("Root")
        }
        if (_.includes(start, tag)) {
            if (struc[struc.length - 1] !== tag) {
                struc.push(tag)
            }
        }
        // EXCEPTION !!!!
        // if (tag == "PRODUCTS" && !tag.close) {
        //     xml.push('</ORDER_INFO></ORDER_HEADER>')
        //     return
        // }
        //---------------- Handle Close Tags -----------------------//
        /** 
         * 
         * 1- SKIP TAG  remove from the controller array
         * 2- Add end tag
         * */
        if (_.includes(skip, tag)) {
            // console.log('Skipping:', tag)
            // console.log('Removing ', struc[struc.length - 1], struc)
            // console.log('===============', getGrammar(struc[struc.length - 1], "tag"))
            // xml.push("</" + struc[struc.length - 1] + ">")
            if (getGrammar(struc[struc.length - 1], "tag")) {
                xml.push("</" + getGrammar(struc[struc.length - 1], "tag") + ">");
            }
            struc.splice(struc.lastIndexOf(tag), 1);
            return
        }
        //----------------------------------------------------------
        // PRODUCTS // SHOULD FIX
        if (tag == "PRODUCTS") {
            console.log('++++++++++++++ PRODUCTS')
            if (jsonElem.close) {
                xml.push("</" + getGrammar(tag, 'tag') + ">")
            } else {
                xml.push('</ORDER_INFO></ORDER_HEADER>')
                xml.push("<" + getGrammar(tag, 'tag') + ">")
            }
            return
        }
        if (tag == "PARTIES") {
            console.log('++++++++++++++ PARTIES')
            if (jsonElem.close) {
                xml.push("</" + getGrammar(tag, 'tag') + ">")
            } else {
                xml.push("<" + getGrammar(tag, 'tag') + ">")
            }
            return
        }
        /* -------------------------------------------------------------------------- */
        // IF HAS CHILDREN
        if (jsonElem.children) {
            if (getGrammar(tag, 'tag')) {
                xml.push("<" + getGrammar(tag, 'tag') + ">")
            }
            var parent = jsonElem
            // console.log("Children Renderring", jsonElem.children.length, jsonElem.tag)
            if (jsonElem.children.length) {
                xml.push(getXMLElement(jsonElem.index))
                _.each(jsonElem.children, (child, i) => {
                    //
                    // SETTING CACULATED DATA: (PRICE_LINE_AMOUNT)
                    if(!child.index){
                        xml.push(child.render)                        
                    }else{
                        xml.push(getXMLElement(child.index))
                    }
                    // console.log("PARENT: ", parent.tag, '->  Child: ', child.tag)
                })
            } else {
                // xml.push(jsonElem.line)
                // xml.push('<!-- has no children NOCHILDREN-->')
                xml.push(getXMLElement(jsonElem.index))
            }
            // KEEP THE EDIFACT META TAG OPEN
            if (!_.includes(keepOpen, jsonElem.tag)) {
                // console.log('keepOpen: ', keepOpen, "</" + jsonElem.tag + ">")
                if (getGrammar(parent.tag, 'tag')) {
                    xml.push("</" + getGrammar(parent.tag, 'tag') + ">")
                }
            }
            //
        } else {
            //IF is PARENT AND has NO Children ex. [NAD]
            // Pushing DATA LINE
            // xml.push("LINEEEEEEEEE")
            //
            xml.push("</" + jsonElem.tag + ">")
        }
    })
    // console.log({
    //     struc
    // })
    xml.push(`
    <ORDER_SUMMARY>
        <TOTAL_ITEM_NUM>11</TOTAL_ITEM_NUM>
        <TOTAL_AMOUNT>1080.25</TOTAL_AMOUNT>
    </ORDER_SUMMARY>
    `)
    xml.push('</ORDER>')
    xml.push('')
    var xml = xml.join("")
    return xml;
}
/* -------------------------------------------------------------------------- */
// OLD XML RENDERING FUNCTION
function renderXML() {
    _.each(tags, (tag, index) => {
        var prev = tag;
        var line = linesArr[index].substring(4, 100)
        var i = index;
        // SKIP UNA
        if (tag == "UNA") {
            return
        }
        if (struc.length == 0 && tag) {
            struc.push("Root")
        }
        // return;
        // IF is Parent
        // LIN: {START} CREATE PRODUCTS ARRAY HEADER
        if (tag == "LIN" && (order[order.length - 1] !== "LIN")) {
            out.push("<PRODUCTS>")
        }
        //
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
                // REMOVED 25 MAY
                // out.push(getXMLElement(index))
                // NEXT IF NOT THE SAME AS THE CURRENT TAG (LIN)
            } else if (struc[struc.length - 1] === tag) {
                // used to close the tag
                out.push("</" + tag + ">")
                out.push("<" + tag + ">")
                // REMOVED
                // out.push(getXMLElement(index))
            }
            var parent = struc[struc.length - 2]
            var k = struc[struc.length - 1]
            // console.log('Tag: ', tag, 'Parent: ', parent, k)
        } else {
            if (ediData[index].matchedData) {
                // console.log('____________ HAS DATA INDEX ___________')
                var x = getXMLElement(index)
                out.push(x)
            } else {
                //  attr="' + line + '"
                out.push('<' + tag + '>' + line + "</" + tag + ">")
            }
            // out.push('<' + tag + ' attr="' + line + '">'+ getXMLElement(tag,index,line) + "</" + tag + ">")
        }
        // // Closing Tag 
        if (_.includes(skip, tag)) {
            out.push("</" + struc[struc.length - 1] + ">")
            if (struc[struc.length - 1] == "LIN") {
                out.push("</PRODUCTS>")
            }
            // console.log('CLOSING: ', "</" + struc[struc.length - 1] + ">")
            // console.log('struc',struc)
            // console.log('************Closing: ', struc[struc.length - 1], tag, "</" + struc[struc.length - 1] + ">")
            // console.log("Removing: ", struc, struc[struc.length - 1])
            struc.splice(struc.lastIndexOf(tag), 1);
            // console.log("Removed: ", struc, tag)
        }
    })
    out.push("</" + order[0] + ">")
    out.push('</ORDER>')
    var xml = out.join("")
    return xml;
}
/* -------------------------------------------------------------------------- */
// REPLACE XML TAGS
// LIN => PRODUCT
parseEDI.setTags = function (xml) {
    for (i = 0; i < Grammar.length; i++) {
        var tag = Grammar[i].name
        let re = new RegExp(`\\b${tag}\\b`, 'g');
        var xml = xml.replace(re, Grammar[i].tag);
    }
    return xml
}
/* -------------------------------------------------------------------------- */
// Render XML // REPLACE THE DATA 
// MATCH DATA WITH RENDERED LINE
function getXMLElement(index) {
    // console.log('getXMLElement',ediData[index].key)
    if (!ediData[index]) {
        return
    }
    var elementsAll = ediData[index].elements
    var line = ediData[index].render
    var data = ediData[index].matchedData
    if (!data) {
        console.error('No Data', ediData[index].key)
        return
    }
    // console.log(ediData[index])
    if (ediData[index].cases && ediData[index].cases[0]) {
        var casee = ediData[index].cases
        var find = _.find(data, (o) => {
            if (o[casee[0]]) {
                return o[casee[0]]
            }
        })
        var key = casee[0];
        var line = ediData[index][find[key]]
        // console.log({
        //     casee,
        //     data,
        //     key,
        //     find,
        //     line
        // })
    }
    // Looping and replacing line:
    for (i = 0; i < data.length; i++) {
        var key = _.keys(data[i])[0]
        if (ediData[index].cases && !key == ediData[index].cases[0]) {
            var line = ediData[index].exc(data[i][key])
        }
        var line = line.replace(key, data[i][key])
    }
    return line
}
/* -------------------------------------------------------------------------- */
/**
 * matchDataBlock
 * GENERATE MatchedBlock for the Data.
 *  USED for SIMPLE TAGS
 * @param {*} dataArr 
 * @param {*} grammarArr 
 */
// matchDataBlock( [ [ '', '220', '0351485311' ], [] ], [["","$code","$id"],[]])
// matchDataBlock( [ [ '', '220', '0351485311' ], ["EU","EURO", "CH"] ], [["","$code","$id"],["$re","$curr", "$country"]])
function matchDataBlock(dataArr, grammarArr) {
    if (!grammarArr || !dataArr) {
        console.log('matchDataBlock: ERROR')
        throw new Meteor.Error('grammarArr Match has error', "error")
    }
    var output = []
    //
    _.each(grammarArr, (elementsArr, index) => {
        _.each(elementsArr, (ele, i) => {
            if (ele.length) {
                output.push({
                    [ele]: dataArr[index][i] ? dataArr[index][i] : ""
                })
            }
        })
    })
    //
    // console.log('matchDataBlock: Output', output)
    return output;
}
/* -------------------------------------------------------------------------- */
//
/* -------------------------------------------------------------------------- */
var xml = renderXML(doc)
/* -------------------------------------------------------------------------- */
// REPLACE XML TAGS
var xml = parseEDI.setTags(xml)
/* -------------------------------------------------------------------------- */
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
// var xml = format(xml);
writeFile('xml-01.xml', xml)
/* -------------------------------------------------------------------------- */
// Match two arrays with Data and Grammer
// matchData(["CODE", 120, "PCS"],["$code","$qty","$unit"])
function matchData(arr, grammar) {
    var arr = arr.map((arrEle, index) => {
        return {
            [grammar[index]]: arrEle
        }
    })
    console.log('matchData: Result', arr)
    return arr;
}
/* -------------------------------------------------------------------------- */
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
function writeFile(file, data) {
    console.log('Writing file..........', file)
    fs.writeFile(meteorPrivate + './' + file, data, 'utf8', function (err) {
        if (err) {
            return console.log(err);
        } else {
            console.log("Writing Mew File [Success]", file)
        }
    });
}
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
function renderJSON() {
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
                var x = getXMLElement(index)
                out.push(x)
            } else {
                //  attr="' + line + '"
                out.push('<' + tag + '>' + line + "</" + tag + ">")
            }
            // out.push('<' + tag + ' attr="' + line + '">'+ getXMLElement(tag,index,line) + "</" + tag + ">")
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
/* -------------------------------------------------------------------------- */
function jsonToXMLxx(jsonArr) {
    var keepOpen = ["UNB", "UNG", "UNH"]
    var skip = ["UNT", "UNE", "UNZ"]
    var xml = []
    var strucJSON = []
    xml.push('<?xml version="1.0" encoding="utf-8" standalone="yes"?><ORDER>')
    _.each(jsonArr, (jsonElem) => {
        /* -------------------------------------------------------------------------- */
        // if (_.includes(start, jsonElem.tag)) {
        //     if (strucJSON.length == 0 && jsonElem.tag) {
        //         strucJSON.push("Root")
        //     }
        //     if (strucJSON[strucJSON.length - 1] !== jsonElem.tag) {
        //         // console.log("New Tag", tag, strucJSON[strucJSON.length - 1])
        //         console.log('PARENT: ', strucJSON[strucJSON.length - 1], '> ', jsonElem.tag)
        //         strucJSON.push(jsonElem.tag)
        //     }
        // }
        // if (_.includes(skip, jsonElem.tag)) {
        //     // console.log('SKIP START', tag, strucJSON)
        //     strucJSON.splice(strucJSON.lastIndexOf(jsonElem.tag), 1);
        // }
        // console.log('_______________',strucJSON)
        // //
        //
        //
        /* -------------------------------------------------------------------------- */
        if (jsonElem.tag == "UNB" || jsonElem.tag == "UNZ") {
            return
        }
        // && jsonElem.parent !== "ROOT" 
        if (jsonElem.close) {
            xml.push("</" + jsonElem.close + ">")
            console.log("*****************closing: </" + jsonElem.close + ">")
            // SKIP after close
            return
        } else {
            xml.push("<" + jsonElem.tag + ">")
            console.log("<" + jsonElem.tag + ">")
        }
        if (jsonElem.children && jsonElem.parent !== "Root" && jsonElem.isParent == true && jsonElem.children.length) {
            var parent = jsonElem
            console.log("Children Renderring", jsonElem.children.length, jsonElem.tag)
            _.each(jsonElem.children, (child, i) => {
                xml.push("<" + child.tag + ">" + child.line + "</" + child.tag + ">")
                console.log("PARENT: ", parent.tag, '->  Child: ', child.tag)
            })
            // console.log("Children Renderring Close", parent.tag)
            // keepOpen
            // KEEP THE EDIFACT META TAG OPEN
            if (!_.includes(keepOpen, jsonElem.tag)) {
                console.log('keepOpen: ', keepOpen, "</" + jsonElem.tag + ">")
                xml.push("</" + parent.tag + ">")
            }
            //
        } else {
            //IF is PARENT AND has NO Children [NAD]
            xml.push(jsonElem.line)
            xml.push("</" + jsonElem.tag + ">")
        }
    })
    xml.push('</ORDER>')
    var xml = xml.join("")
    return xml;
}
/* -------------------------------------------------------------------------- */
function jsonToXMLX(jsonArr) {
    console.log('-----------------')
    var xml = []
    _.each(jsonArr, (jsonElem) => {
        var index = jsonElem.index
        var element = ediData[index]
        if (!element) {
            return
        }
        // OPEN and CLOSE PARENT TAGS
        if (jsonElem.close) {
            xml.push("</" + jsonElem.close + ">")
            console.log("</" + jsonElem.close + ">")
        } else {
            xml.push("<" + jsonElem.tag + ">")
            console.log("</" + jsonElem.tag + ">")
        } //
        if (jsonElem.children && jsonElem.children.length > 0 && jsonElem.parent !== "Root") {
            var parent = jsonElem
            // console.log("Children Renderring", jsonElem.children.length, jsonElem.tag)
            _.each(jsonElem.children, (child, i) => {
                // console.log(child,i)
                xml.push("<" + child.tag + ">" + child.line + "</" + child.tag + ">")
                // console.log("PARENT: ", parent.tag, '->  Child: ', child.tag)
            })
            // console.log("Children Renderring Close", "jsonElem.children.length", parent.tag)
            xml.push("</" + parent.tag + ">")
            console.log("</" + jsonElem.tag + ">")
            // CHECK the children length while looping and close on the end
            //
        }
        // elementsAll,line,data,rendered,
        // console.log('----')
        // console.log({
        //     element
        // })
        if (jsonElem.tag == "UNB") {
            console.log(jsonElem)
        }
    })
    console.log('-----------------')
    var xml = xml.join("")
    // console.log({
    //     xml
    // })
    return xml
}
//
/* -------------------------------------------------------------------------- */
function generateStructuredJSON() {
    var arr = []
    strucJSON = [] //
    var orderJSON = []
    var parent = ["UNB", "UNG", "UNH", "LIN", "NAD", "CUX"]
    var start = ["UNB", "UNG", "UNH", "LIN"]
    var skip = ["UNT", "UNE", "UNZ"]
    var looped = ["LIN", "NAD"] //
    _.each(tags, (tag, index) => {
        var line = linesArr[index].substring(4, 100)
        var el = {
            tag: tag,
            line: line,
            index: index
        }
        // Controller Arr
        if (_.includes(start, tag)) {
            if (strucJSON.length == 0 && tag) {
                strucJSON.push("Root")
            }
            if (strucJSON[strucJSON.length - 1] !== tag) {
                // console.log("New Tag", tag, strucJSON[strucJSON.length - 1])
                console.log('PARENT: ', strucJSON[strucJSON.length - 1], '> ', tag)
                strucJSON.push(tag)
            } else if (strucJSON[strucJSON.length - 1] === tag) {
                // console.log("LOOP: =======================", tag, strucJSON[strucJSON.length - 1])
            }
        }
        if (_.includes(skip, tag)) {
            // console.log('SKIP START', tag, strucJSON)
            strucJSON.splice(strucJSON.lastIndexOf(tag), 1);
            // console.log('SKIP END RM', tag, strucJSON)
            // orderJSON.push(tag)
            // Adding Skipp Tags 
            el.close = strucJSON[strucJSON.length - 1]
            el.skip = true
            // return
        }
        if (_.includes(parent, tag)) {
            orderJSON.push(tag)
            el.parent = strucJSON[strucJSON.length - 2]
            el.isParent = true
        } else {
            parentTag = strucJSON[strucJSON.length - 1]
            if (!parentTag) {
                return
            }
            el.parentTag = parentTag;
            el.isChild = true
        }
        arr.push(el)
    })
    var arr = setEnclosedTags(arr, "NAD", "PARTIES")
    var arr = setEnclosedTags(arr, "LIN", "PRODUCTS")
    return arr
}
writeFile('json.export.lines.json', JSON.stringify(generateStructuredJSON()))
/* -------------------------------------------------------------------------- */
// Testing
// getGrammar('BGM', "tag")
// getRenderedData(15)
// console.log(getXMLElement(15))