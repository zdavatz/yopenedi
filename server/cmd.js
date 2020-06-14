/**
 * CMD
 */


var child_process = require('child_process');

cmd = {}

cmd.run = (command)=>{
    var result = child_process.execSync(command);
    return result;
}

module.exports = cmd