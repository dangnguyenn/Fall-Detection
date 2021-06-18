var threshHold = 0.3;
var sampleLimit = 35;
var alphaLimit = 20;
var count = 0;
var isFall = false;
var betaQueue;
var alphaQueue;
var heuristicsQueue;
var TNQueue = {"data":[], "timestamp":[]};

exports.setup = function() {
    //uni-directional dataCollection-prediction
    this.input('accelerometerReceive');
    //uni-directional prediction-database
    this.output('labeledAccelerometerSend');
};

exports.initialize = function() {
    this.addInputHandler('accelerometerReceive', predicts.bind(this));
    betaQueue = new Queue();
    alphaQueue = new Queue();
    heuristicsQueue = new Queue();
};

function predicts(){
    var self = this;
    var output = 0.0;
    var sample = this.get('accelerometerReceive');
    var x = sample.document.x;
    var y = sample.document.y;
    var z = sample.document.z;
    var timestamp = sample.document.timestamp;

    if ( alphaQueue.size() >= alphaLimit ){
        // perform final prediction

        var heuristics = Object.create(heuristicsQueue);
        while(!heuristics.isEmpty()) {
            output += heuristics.poll();
        }
        output = output / alphaLimit;

        if (output < threshHold) {
            isFall = false;

            var removedBetaQueue = alphaQueue.poll();
            var removedSample = removedBetaQueue.poll();

            if(TNQueue.data.length <= 375) {
                TNQueue.data.push(removedSample.data);
                TNQueue.timestamp.push(removedSample.timestamp);
            }
            else {
                TNQueue.label = "TN";
                var doc = {"databaseName": "accelerometer", "document": TNQueue};
                self.send('labeledAccelerometerSend', doc);

                TNQueue = {"data":[], "timestamp":[]};
            }

            heuristicsQueue.poll();

            /*
            doc object structure:
            doc = { "databaseName": "accelerometer",
                    "document": {"data" : [[x,y,z],...], "timestamp": [timestamp,...], "label": "TN"}
                    }
            */
        }
        else if (output >= threshHold && !isFall) {
            var samples = {"data":[], "timestamp":[]}
            while(!alphaQueue.isEmpty()) {
                if(alphaQueue.size() != 1) {
                    var removedBetaQueue = alphaQueue.poll();
                    var removedSample = removedBetaQueue.poll();
                    samples.data.push(removedSample.data);
                    samples.timestamp.push(removedSample.timestamp);
                }
                else {
                    var lastBeta = alphaQueue.poll();
                    while(!lastBeta.isEmpty()) {
                        var removedSample = lastBeta.poll();
                        samples.data.push(removedSample.data);
                        samples.timestamp.push(removedSample.timestamp);
                    }
                }
            }

            samples.label = "??";

            var doc = {"databaseName": "accelerometer", "document": samples};
            self.send('labeledAccelerometerSend', doc);

            reset();

            isFall = true;
        }

    }
    var newSample = {"data":[x,y,z], "timestamp": timestamp}
    enqueueSample(newSample);
}

function enqueueSample(newSample){
    // if beta queue is full (35 samples):
    //          - push to alpha
    //          - make and push inference to heuristics
    //          - remove oldest from beta and add the new sample

    if (betaQueue.size() >= sampleLimit) {
        enqueueBeta(); // push to alpha, make and push inference to heuristics
        betaQueue.poll(); // remove oldest from beta
    }
    betaQueue.offer(newSample); // add the new sample to beta
}

function enqueueBeta(){
    // add the beta to the alpha
    var betaQueueSamples = [];
    var tempBeta = Object.create(betaQueue);
    for(var i = 0; i < sampleLimit; i++){
        var sample = tempBeta.poll();
        betaQueueSamples.push(sample.data);
    }

    alphaQueue.offer(betaQueue);

    prediction.predict(
        betaQueueSamples,
        function(result){
            heuristicsQueue.offer(result);
            count++;
            document.querySelector('#message').innerHTML = "count: " + count;
        },
        function(error){console.log(error);}
    );

}

// if TP/FP
function reset(){
    alphaQueue.clear();
    betaQueue.clear();
    heuristicsQueue.clear();
}


/*
    Queue data structure implementation
*/
function Node(val){
    this.val = val;
    this.next = null;
}

function Queue(){
    this.head = null;
    this.tail = null;
    this.length = 0;
//    Object.assign(this, {null, null, 0});
}

Queue.prototype.size = function sizes() {
    return this.length;
}

Queue.prototype.isEmpty = function isEmpty() {
    return !this.length
}

Queue.prototype.offer = function offer(val) {
    let newNode = new Node(val);
    if(!this.head){
        this.head = newNode
        this.tail = newNode
    }
    else{
        this.tail.next = newNode;
        this.tail = newNode;
    }
    ++this.length;
}

Queue.prototype.peek = function peek() {
    if(this.isEmpty())
        return null;
    else
        return this.head.val;

}

Queue.prototype.poll = function poll() {
    if(this.isEmpty()) return null;

    const temp = this.head;
    if(this.head == this.tail) this.tail = null;

    this.head = this.head.next;
    --this.length;

    return temp.val;
}

Queue.prototype.clear = function clear() {
    this.head = null;
    this.tail = null;
    this.length = 0;
    return true;
}