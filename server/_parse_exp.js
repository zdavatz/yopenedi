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