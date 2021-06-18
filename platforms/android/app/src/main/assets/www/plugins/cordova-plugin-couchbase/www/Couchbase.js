cordova.define("cordova-plugin-couchbase.Couchbase", function(require, exports, module) {
var exec = require('cordova/exec');

module.exports = {
    createNewDatabase: function (arg0, success, error) {
        cordova.exec(success, error, 'Couchbase', 'createNewDatabase', [arg0]);
    },
    insertDocument: function (arg0, arg1, success, error) {
        cordova.exec(success, error, 'Couchbase', 'insertDocument', [arg0, arg1]);
    },
    getAllDocuments: function (arg0, success, error) {
        cordova.exec(success, error, 'Couchbase', 'getAllDocuments', [arg0]);
    },
    updateDocument: function (arg0, arg1, arg2, success, error) {
        cordova.exec(success, error, 'Couchbase', 'updateDocument', [arg0, arg1, arg2]);
    },
    uploadDocuments: function (arg0, arg1, success, error) {
        cordova.exec(success, error, 'Couchbase', 'uploadDocuments', [arg0, arg1]);
    }

};
});