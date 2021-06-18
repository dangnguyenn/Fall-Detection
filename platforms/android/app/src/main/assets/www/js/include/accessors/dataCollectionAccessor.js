var _docId;

exports.setup = function() {
     //uni-directional UI-dataCollection
     this.input('sendMessage');
     //bi-directional dataCollection-prediction
     this.output('accelerometerSend', {spontaneous: true, 'type': 'object'});
     this.input('fallDetectedReceive');
};

exports.initialize = function() {
    var self = this;
    this.addInputHandler('sendMessage', sendMessage.bind(this));
    this.addInputHandler('fallDetectedReceive', fallDetected.bind(this));

//  Subscribe for accelerometer data
    function subscribe(fn){
        wearOS.subscribe("/fall",
                        "Fall Data",
                        function(response){
                            fn(response);
                        },
                        function(error){console.log(error);});
    };

    subscribe(function(result){
        var json = JSON.parse(result);
        var document = {"databaseName": "accelerometer", "document": json};
        if(json.path.includes("fall_data")){
            self.send('accelerometerSend', document);
        }
        else if(json.path.includes("fall_label")){
            //TODO make a port to databaseAccessor to update ?? label
            couchbase.updateDocument("accelerometer", _docId, {"label": json.message}, function(response){},function(error){console.log(error);});

            var message = {"message" : "start"};
            wearOS.sendMessage(message, "/path", function(response){}, function(error){console.log(error)});

        }
    });
};

function sendMessage() {
    var message = this.get('sendMessage');
    wearOS.sendMessage(message, "/path", function(response){}, function(error){console.log(error)});
}

function fallDetected() {
    var message = this.get('fallDetectedReceive');
    _docId = message.docId;
    wearOS.sendMessage(message, "/fall_detected", function(response){}, function(error){console.log(error)});
}