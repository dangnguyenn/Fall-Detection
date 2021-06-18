exports.setup = function() {
    couchbase.createNewDatabase("userDatabase", function(response){ },function(error){console.log(error);});
    couchbase.createNewDatabase("activityDatabase", function(response){ },function(error){console.log(error);});
    couchbase.createNewDatabase("accelerometer", function(response){ document.querySelector('#message').innerHTML = "acc: "+ response; },function(error){console.log(error);});

    //uni-directional UI-database
    this.input('documentReceive');
    //bi-directional UI-database : profile
    this.input('loadReceive');
    this.output('profilesSend', {spontaneous: true, 'type': 'object'});
    //bi-directional UI-database : activity
    this.input('getActivityReceive');
    this.output('activitySend', {spontaneous: true, 'type': 'object'});
    //uni-directional prediction-database
    this.input('labeledAccelerometerReceive');
    //uni-directional UI-database
    this.input('updatedDocumentReceive');
    //uni-directional database-dataCollection
    this.output('fallDetectedSend');
};

exports.initialize = function() {
    this.addInputHandler('documentReceive', documentInsert.bind(this));
    this.addInputHandler('loadReceive', loadProfiles.bind(this));
    this.addInputHandler('getActivityReceive', loadActivity.bind(this));
    this.addInputHandler('labeledAccelerometerReceive', accelerometerInsert.bind(this));
    this.addInputHandler('updatedDocumentReceive', updateDocument.bind(this));

//    //upload accelerometer data to server every 900,000 ms (15 min)
//    setInterval(uploadToServer, 900000);
};

function uploadToServer() {
    couchbase.uploadDocuments('http://eil.cs.txstate.edu/couchbase-dev/uploadcouch.php',
                                "accelerometer",
                                function(response){ },
                                function(error){console.log(error);})
}

function documentInsert() {
    var data = this.get('documentReceive');
    couchbase.insertDocument(data.databaseName, data.document, function(response){},function(error){console.log(error);});
}

function accelerometerInsert() {
    var self = this;
    var data = this.get('labeledAccelerometerReceive');
    couchbase.insertDocument(data.databaseName,
                                data.document,
                                function(response){
                                    if(data.document.label == "??") {
                                        var message = {"message" : "fall", "docId": response};
                                        self.send('fallDetectedSend', message);
                                    }
                                },
                                function(error){console.log(error);});
}

function loadProfiles() {
    var self = this;
    var databaseName = this.get('loadReceive');
    function getDocs(fn) {
        couchbase.getAllDocuments(databaseName,
                                    function(objects) {
                                         var json_string = String.fromCharCode.apply(null, new Uint8Array(objects));
                                         json_objects = JSON.parse(json_string);
                                         fn(json_objects);},
                                    function(error){console.log(error);});
    }

    getDocs(function(result) {
        self.send('profilesSend', result);
    });
}

function loadActivity() {

    var self = this;
    var databaseName = this.get('getActivityReceive');

    function getDocs(fn){
        couchbase.getAllDocuments(databaseName,
                                    function(objects) {
                                         var json_string = String.fromCharCode.apply(null, new Uint8Array(objects));
                                         json_objects = JSON.parse(json_string);
                                         fn(json_objects);},
                                    function(error){console.log(error);});
    }

    getDocs(function(result){
        self.send('activitySend', result);
    });
}

function updateDocument() {
    var data = this.get('updatedDocumentReceive');
    couchbase.updateDocument(data.databaseName, data.documentId, data.updatedDocument, function(response){},function(error){console.log(error);});
}