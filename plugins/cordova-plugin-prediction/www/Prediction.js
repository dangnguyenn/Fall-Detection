cordova.define("cordova-plugin-prediction.Prediction", function(require, exports, module) {
var exec = require('cordova/exec');

exports.predict = function (arg0, success, error) {
    exec(success, error, 'Prediction', 'predict', [arg0]);
};

});