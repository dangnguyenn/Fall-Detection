cordova.define("cordova-plugin-wearOS.WearOS", function(require, exports, module) {
var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'WearOS', 'coolMethod', [arg0]);
};

});
