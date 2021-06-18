cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "cordova-plugin-wearOS.WearOS",
      "file": "plugins/cordova-plugin-wearOS/www/WearOS.js",
      "pluginId": "cordova-plugin-wearOS",
      "clobbers": [
        "cordova.plugins.WearOS"
      ]
    },
    {
      "id": "cordova-plugin-couchbase.Couchbase",
      "file": "plugins/cordova-plugin-couchbase/www/Couchbase.js",
      "pluginId": "cordova-plugin-couchbase",
      "clobbers": [
        "cordova.plugins.Couchbase"
      ]
    },
    {
      "id": "cordova-plugin-prediction.Prediction",
      "file": "plugins/cordova-plugin-prediction/www/Prediction.js",
      "pluginId": "cordova-plugin-prediction",
      "clobbers": [
        "prediction"
      ]
    }
  ];
  module.exports.metadata = {
    "cordova-plugin-whitelist": "1.3.4",
    "cordova-plugin-wearOS": "0.0.1",
    "cordova-plugin-couchbase": "0.0.1",
    "cordova-plugin-prediction": "0.0.1"
  };
});