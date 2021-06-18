cordova.define("cordova-plugin-wearOS.WearOS", function(require, exports, module) {
var exec = require('cordova/exec');

exports.subscribe = function (arg0, arg1, success, error) {
    exec(success, error, 'WearOS', 'subscribe', [arg0, arg1]);
};
exports.sendMessage = function (arg0, arg1, success, error) {
    exec(success,error, 'WearOS', 'sendMessage', [arg0, arg1]);
};
});
