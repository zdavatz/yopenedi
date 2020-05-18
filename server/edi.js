var EDI = function (string) {
    this.string = string;
}

/* Generic EDIFACT functions */

// EDI parse lines
EDI.prototype.lines = function () {
    var lines = this.string.split(/['\n\r]+/);
    lines = lines.map(function (line) {
        // console.log('Line',line)
        return (new EDI(line));
    });
    return (lines);
}

// EDI parse segments
EDI.prototype.segments = function (token) {
    var segments = this.string.split(token);
    segments = segments.splice(1, segments.length)
    segments = segments.map(function (text) {
        return (new EDI(token + text));
    });
    return (segments);
}

// search for segment
EDI.prototype.segment = function (token) {
    var esc = token.replace("+", "\\+");
    var search = new RegExp(esc + "\+[^\']+", "g");
    var segment = search.exec(this.string);
    if (!segment || !segment[0]) segment = "";
    else segment = "" + segment[0];
    return (new EDI(segment));
}

// return n'th element (zero-index)
EDI.prototype.element = function (n) {
    // var elements = this.string.split('+');
    // split while handling escape characters: credits: http://stackoverflow.com/a/14334054
    var elements = this.string.match(/(\?.|[^\+])+/g)
    var element = "";
    if (!elements || n > elements.length - 1) element = "";
    else element = elements[n];
    return (new EDI(element));
}

// return n'th component (zero-index)
EDI.prototype.component = function (n) {
    // var components = this.string.split(':');
    // split while handling escape characters: credits: http://stackoverflow.com/a/14334054
    var components = this.string.match(/(\?.|[^:])+/g)
    var component = "";
    if (!components || n > components.length - 1) component = "";
    else component = components[n];
    return (new EDI(component));
}

EDI.prototype.toString = function () {
    return ("" + this.string)
}

EDI.prototype.toNumber = function () {
    return (parseFloat(this.toString()))
}

// EDI.prototype.valueOf = function(){
//   return(this.string)
// }

// Extract batches from EDI message
EDI.prototype.bsegments = function () {
    var bsegments = this.string.match(/UNB\+.*?UNZ\+[^\']+?/g);
    bsegments = bsegments.map(function (segment) {
        return (new EDI(segment));
    });
    return (bsegments);
}

// Extract messages
EDI.prototype.msegments = function () {
    var msegments = this.string.match(/UNH\+.*?UNT\+[^\']+?/g);
    msegments = msegments.map(function (segment) {
        return (new EDI(segment));
    });
    return (msegments);
}

/* External libraries for *isotime() functions */
var moment = require('moment');
require('twix');

/* Batch specific functions */
EDI.prototype.bfrom = function () {
    return (this.segment('UNB').element(2).component(0).toString());
}
EDI.prototype.bto = function () {
    return (this.segment('UNB').element(3).component(0).toString());
}
EDI.prototype.btime = function () {
    return (this.segment('UNB').element(4).toString());
}
EDI.prototype.bisotime = function () {
    return (moment(this.btime(), 'YYMMDD:HHmm').format().toString());
}

/* Message specific functions */
EDI.prototype.mid = function () {
    return (this.segment('UNH').element(1).toString());
}
EDI.prototype.mtype = function () {
    return (this.segment('UNH').element(2).component(0).toString());
}
EDI.prototype.msubtype = function () {
    return (this.segment('BGM').element(1).component(0).toString());
}
EDI.prototype.mref = function () {
    return (this.segment('BGM').element(2).toString());
}
EDI.prototype.mfrom = function () {
    return (this.segment('NAD+MS').element(2).component(0).toString());
}
EDI.prototype.mto = function () {
    return (this.segment('NAD+MR').element(2).component(0).toString());
}
EDI.prototype.mproduct = function () {
    return (this.segment('MKS').element(1).toString());
}
EDI.prototype.mtime = function () {
    return (this.segment('DTM+137').element(1).component(1).toString());
}
EDI.prototype.moffset = function () {
    return (this.segment('DTM+735').component(1).toString().replace('?', '').toString());
}
EDI.prototype.misotime = function () {
    return (moment(this.mtime() + this.moffset(), "YYYYMMDDHHmmZZ").format().toString());
}

/* Unit testing */
EDI.prototype.test = function () {

    var err = [];

    var text = "UNA:+.? 'UNB+UNOC:3+9999999999918:14+9999999999925:14+120520:1652+11++STS++1++1'UNH+DGONR06606+UTILTS:D:07B:UN:E5BE05'BGM+E26::260+ABC00104110014+9+NA'DTM+137:201205201402:203'DTM+735:?+0100:406'MKS+23'PRC+E43:BEL:260+BC1:BEL:260'RFF+AWG:5499780282808000001'RFF+ADD:C51-T01'NAD+MS+9999999999918::9'NAD+MR+9999999999925::9'IDE+24+TR3264101'NAD+DDK+5499773453543::9'LIN+1++8716867000030:::9'DTM+324:201007312300201112010000:719'DTM+354:1:802'STS+7++E43::260'MEA+AAZ++KWH'CCI+++E01::260'CAV+S10:BEL:260'CCI+++E12::260'CAV+E17::260'CCI+++E14::260'CAV+E13::260'SEQ++1'QTY+Z01:200.00'SEQ++2'QTY+Z01:200.00'SEQ++3'QTY+Z01:200.00'SEQ++4'QTY+Z01:200.00'SEQ++5'QTY+Z01:200.00'SEQ++6'QTY+Z01:200.00'SEQ++7'QTY+Z01:200.00'SEQ++8'QTY+Z01:200.00'SEQ++9'QTY+Z01:200.00'SEQ++10'QTY+Z01:200.00'SEQ++11'QTY+Z01:200.00'SEQ++12'QTY+Z01:200.00'SEQ++13'QTY+Z01:200.00'SEQ++14'QTY+Z01:200.00'SEQ++15'QTY+Z01:200.00'SEQ++16'QTY+Z01:200.00'IDE+24+TR5741386'NAD+DDK+5499779205603::9'LIN+1++8716867000030:::9'DTM+324:201007312300201112010000:719'DTM+354:1:802'STS+7++E43::260'MEA+AAZ++KWH'CCI+++E01::260'CAV+S10:BEL:260'CCI+++E12::260'CAV+E17::260'CCI+++E14::260'CAV+E13::260'SEQ++1'QTY+Z01:200.00'SEQ++2'QTY+Z01:200.00'SEQ++3'QTY+Z01:200.00'SEQ++4'QTY+Z01:200.00'SEQ++5'QTY+Z01:200.00'SEQ++6'QTY+Z01:200.00'SEQ++7'QTY+Z01:200.00'SEQ++8'QTY+Z01:200.00'SEQ++9'QTY+Z01:200.00'SEQ++10'QTY+Z01:200.00'SEQ++11'QTY+Z01:200.00'SEQ++12'QTY+Z01:200.00'SEQ++13'QTY+Z01:200.00'SEQ++14'QTY+Z01:200.00'SEQ++15'QTY+Z01:200.00'SEQ++16'QTY+Z01:200.00'IDE+24+TR3264102'NAD+DDK+5499779205603::9'LIN+1++8716867000030:::9'DTM+324:201007312300201112010000:719'DTM+354:1:802'STS+7++E43::260'MEA+AAZ++KWH'CCI+++E01::260'CAV+S11:BEL:260'CCI+++E12::260'CAV+E17::260'CCI+++E14::260'CAV+B17:BEL:260'SEQ++1'QTY+Z01:200.00'SEQ++2'QTY+Z01:200.00'SEQ++3'QTY+Z01:200.00'SEQ++4'QTY+Z01:200.00'SEQ++5'QTY+Z01:200.00'SEQ++6'QTY+Z01:200.00'SEQ++7'QTY+Z01:200.00'SEQ++8'QTY+Z01:200.00'SEQ++9'QTY+Z01:200.00'SEQ++10'QTY+Z01:200.00'SEQ++11'QTY+Z01:200.00'SEQ++12'QTY+Z01:200.00'SEQ++13'QTY+Z01:200.00'SEQ++14'QTY+Z01:200.00'SEQ++15'QTY+Z01:200.00'SEQ++16'QTY+Z01:200.00'IDE+24+TR3264103'NAD+DDK+5499779205603::9'LIN+1++8716867000030:::9'DTM+324:201007312300201112010000:719'DTM+354:1:802'STS+7++E43::260'MEA+AAZ++KWH'CCI+++E01::260'CAV+S11:BEL:260'CCI+++E12::260'CAV+E17::260'CCI+++E14::260'CAV+B18:BEL:260'SEQ++1'QTY+Z01:200.00'SEQ++2'QTY+Z01:200.00'SEQ++3'QTY+Z01:200.00'SEQ++4'QTY+Z01:200.00'SEQ++5'QTY+Z01:200.00'SEQ++6'QTY+Z01:200.00'SEQ++7'QTY+Z01:200.00'SEQ++8'QTY+Z01:200.00'SEQ++9'QTY+Z01:200.00'SEQ++10'QTY+Z01:200.00'SEQ++11'QTY+Z01:200.00'SEQ++12'QTY+Z01:200.00'SEQ++13'QTY+Z01:200.00'SEQ++14'QTY+Z01:200.00'SEQ++15'QTY+Z01:200.00'SEQ++16'QTY+Z01:200.00'IDE+24+TR3896501'NAD+DDK+5499779205603::9'LIN+1++8716867000030:::9'DTM+324:201007312300201112010000:719'DTM+354:1:802'STS+7++E43::260'MEA+AAZ++KWH'CCI+++E01::260'CAV+S10:BEL:260'CCI+++E12::260'CAV+E18::260'CCI+++E14::260'CAV+E13::260'SEQ++1'QTY+Z01:200.00'SEQ++2'QTY+Z01:200.00'SEQ++3'QTY+Z01:200.00'SEQ++4'QTY+Z01:200.00'SEQ++5'QTY+Z01:200.00'SEQ++6'QTY+Z01:200.00'SEQ++7'QTY+Z01:200.00'SEQ++8'QTY+Z01:200.00'SEQ++9'QTY+Z01:200.00'SEQ++10'QTY+Z01:200.00'SEQ++11'QTY+Z01:200.00'SEQ++12'QTY+Z01:200.00'SEQ++13'QTY+Z01:200.00'SEQ++14'QTY+Z01:200.00'SEQ++15'QTY+Z01:200.00'SEQ++16'QTY+Z01:200.00'UNT+236+DGONR06606'UNZ+1+11'";
    var msg = new EDI(text);
    if (!msg) err.push('new String() failed.');

    var batches = msg.bsegments();
    if (!batches || batches.length != 1) err.push('EDI.prototype.bsegments() failed.');

    var batch = batches[0];
    if (batch.bfrom() != '9999999999918') err.push('EDI.prototype.bfrom() failed.');
    if (batch.bto() != '9999999999925') err.push('EDI.prototype.bto() failed.');
    if (batch.btime() != '120520:1652') err.push('EDI.prototype.btime() failed.');
    if (batch.bisotime() != '2012-05-20T16:52:00+02:00') err.push('EDI.prototype.bisotime() failed.');

    var messages = batches[0].msegments();
    if (!messages || messages.length != 1) err.push('EDI.prototype.msegments() failed.');

    var message = messages[0];
    if (message.mid() != "DGONR06606") err.push('EDI.prototype.mid(): failed.');
    if (message.mtype() != "UTILTS") err.push('EDI.prototype.mtype(): failed.');
    if (message.msubtype() != "E26") err.push('EDI.prototype.msubtype(): failed.');
    if (message.mref() != "ABC00104110014") err.push('EDI.prototype.mref(): failed.');
    if (message.mtime() != "201205201402") err.push('EDI.prototype.mtime(): failed.');
    if (message.moffset() != "+0100") err.push('EDI.prototype.moffset(): failed.');
    if (message.misotime() != "2012-05-20T15:02:00+02:00") err.push('EDI.prototype.misotime(): failed.');
    if (message.mproduct() != "23") err.push('EDI.prototype.mproduct(): failed.');
    if (message.mfrom() != "9999999999918") err.push('EDI.prototype.mfrom(): failed.');
    if (message.mto() != "9999999999925") err.push('EDI.prototype.mto(): failed.');

    var segments = message.segments('IDE');
    if (!segments || segments.length != 5) err.push('EDI.prototype.segments(): failed.');

    return (err);

}

// Aliases
EDI.prototype.e = EDI.prototype.elem = EDI.prototype.element;
EDI.prototype.c = EDI.prototype.comp = EDI.prototype.component;
EDI.prototype.s = EDI.prototype.str = EDI.prototype.toString;
EDI.prototype.n = EDI.prototype.num = EDI.prototype.toNumber;

exports = module.exports = EDI;