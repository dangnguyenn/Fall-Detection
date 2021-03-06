// Cordova host swarmlet test
//
// Copyright (c) 2016-2017 The Regents of the University of California.
// All rights reserved.
//
// Permission is hereby granted, without written agreement and without
// license or royalty fees, to use, copy, modify, and distribute this
// software and its documentation for any purpose, provided that the above
// copyright notice and the following two paragraphs appear in all copies
// of this software.
//
// IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
// FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
// ENHANCEMENTS, OR MODIFICATIONS.
//

/** This "swarmlet" example, running on Cordova, illustrates the use of Cordova
 *  Host to run a swarmlet. The swarmlet loads loads a 'TestSpontaneousOnce' and 
 *  a 'Hello' accessors, initializes them, and connect them
 *  
 *  See https://www.icyphy.org/accessors/wiki/Main/CordovaHost2
 *  
 *  @module swarmlet.js
 *  @author Victor Nouvellet
 *  @version $$Id: swarmlet.js 1502 2017-04-17 21:34:03Z cxh $$
 */



exports.setup = function() {
    // Instantiate accessors
    var database = this.instantiate('database', 'databaseAccessor');
    var dataCollection = this.instantiate('dataCollection', 'dataCollectionAccessor');
    var UI = this.instantiate('UI', 'UIAccessor');
    var prediction = this.instantiate('prediction', 'predictionAccessor');

    // Connect the accessors' inputs and outputs
    //uni-directional UI-database
    this.connect(UI, 'documentSend', database, 'documentReceive');
    //bi-directional UI-database : profile
    this.connect(UI, 'loadSend', database, 'loadReceive');
    this.connect(database, 'profilesSend', UI, 'profilesReceive');
    //uni-directional UI-dataCollection
    this.connect(UI, 'activitySend', dataCollection, 'sendMessage');
    //bi-directional UI-database : activity
    this.connect(UI, 'getActivitySend', database, 'getActivityReceive');
    this.connect(database, 'activitySend', UI, 'activityReceive');
    //uni-directional dataCollection-prediction
    this.connect(dataCollection, 'accelerometerSend', prediction, 'accelerometerReceive');
    //uni-directional prediction-database
    this.connect(prediction, 'labeledAccelerometerSend', database, 'labeledAccelerometerReceive');
    //uni-directional UI-database
    this.connect(UI, 'updatedDocumentSend', database, 'updatedDocumentReceive');
    //uni-directional database-dataCollection
    this.connect(database, 'fallDetectedSend', dataCollection, 'fallDetectedReceive');

};

exports.initialize = function () {

};

