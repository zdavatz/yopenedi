var child_process = require('child_process');

var runCommand = function(cmd) {
    console.log('Running Command (run.external): ', {
      cmd
    })
    var result = child_process.execSync(cmd);
    var result = result.toString('UTF8');
    return result;
}

module.exports = runCommand;