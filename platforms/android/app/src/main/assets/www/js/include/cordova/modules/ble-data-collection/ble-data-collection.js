// Below is the copyright agreement for the Ptolemy II system.
//
// Copyright (c) 2015-2016 The Regents of the University of California.
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
//
// Ptolemy II includes the work of others, to see those copyrights, follow
// the copyright link on the splash page or see copyright.htm.

/**
 * Module for BLE discovery and connection.
 *
 * @module ble
 * @author Colin Campbell, Ryan Wakabayashi
 * @version $$Id:
 */

// Object to hold the connected device
var connectedDevice = {};


exports.requiredPlugins = ['cordova-plugin-ble'];

exports.connect = function (successCallback, errorCallback) {

    // Dictionary of found devices
    var devices = {};
    // Prevents duplicate devices from being displayed
    var processed = [];

    // Start scan allows us to scan for advertised ble devices and store them in our dictionary
    function startScan(fn){ //

          // Cordova ble plugin function call
          evothings.ble.startScan(
               function(device)
               {
                   // add each found device to the dictionary
                   device.timeStamp = Date.now();
                   devices[device.address] = device;
                   fn(devices);
               },
               function(error)
               {
                   console.log('BLE scan error: ' + error)
                   errorCallback();
               });

    }

    // Display devices allows us to display a button for each found ble device
    function displayDevices(devices){

            // Dynamically create a button for each device in the dictionary
            for (var key in devices)
            {
                var device = devices[key];
                var name = device.name || 'no name';

                // Only display devices with a name and ignore repeats
                if(name != 'no name' && !(processed.includes(name))){

                    // add device to processed list
                    processed.push(name);

                    // Create the a button for the device
                    var button = document.createElement("button");
                    button.innerHTML = name;
                    button.setAttribute('id', device.address);
                    button.style.padding = '8px';
                    button.setAttribute( 'class', "btn btn-outline-dark btn-block");

                    // If the device is selected, stop scanning and proceed to connect to it
                    button.onclick = function(d){

                        evothings.ble.stopScan();
                        document.querySelector('#found-devices').innerHTML = '';
                        document.querySelector('#message').innerHTML = 'Connecting to device: ' + devices[d.target.id].name;
                        connect(devices[d.target.id]);

                    };

                    // Add the button to the html document
                    document.querySelector('#found-devices').appendChild(button);
                    document.querySelector('#found-devices').appendChild(document.createElement('br'));
                }
            }
    }

    // Connect receives a device object and connects to it
    function connect (device){

         // Cordova ble plugin function call
         evothings.ble.connectToDevice(
                   device,
                   function(device){

                     // Upon connecting to the device save the object
                     document.querySelector('#message').innerHTML = 'Connected to device: ' + device.name;
                     connectedDevice.device = device;
                   },
                   function(device){
                     console.log('Disconnected from device: ' + device.name);
                   },
                   function(errorCode){
                     console.log('Connect error: ' + errorCode);
                     errorCallback();
             });

    }

    // Start scan function call that initiates our scan
    // and links our found devices to our display function
    startScan(function(devices){

         displayDevices(devices);

    });

};


exports.subscribe = function(successCallback, errorCallback, sensor_type){

    // Enter a state of wait until the device is connected
    var timer = setInterval(function(){
                if(connectedDevice.device != null){
                    clearInterval(timer);
                    setTimeout(getServices, 2000);
                    }}, 500);

    // Get devices advertised services
   function getServices(){

        evothings.ble.readAllServiceData(
                 connectedDevice.device,
                 function(services){
                    // Save the services
                    connectedDevice.services = services;
                    // Get the characteristic
                    getCharacteristic();
                 },
                 function(errorCode){
                    console.log('Services error: ' + errorCode);
                    errorCallback();
                 });

   }

   // Get the characteristics of each service
   function getCharacteristic(){

        for (var sIndex in connectedDevice.services){
            var service = connectedDevice.services[sIndex];
            for (var cIndex in service.characteristics){
                var characteristic = service.characteristics[cIndex];
                getDescriptors(characteristic);
            }
        }

   }

   // Get the descriptor of a characteristic
   function getDescriptors(characteristic){

        evothings.ble.descriptors(
                connectedDevice.device,
                characteristic,
                function(descriptors)
                {
                    // Check the value of each descriptor
                    for( var i in descriptors){
                        var descriptor = descriptors[i];
                        // If the value matches the sensor type, subscribe to the characteristic
                        discoverSensor(descriptor,function(){
                            connectedDevice.characteristic = characteristic;
                            getData();
                        });
                    }
                    console.log('found descriptors:');

                },
                function(errorCode)
                {
                    console.log('descriptors error: ' + errorCode);
                });

   }
   // Check to see if the descriptor value matches the sensor type
   function discoverSensor(descriptor, subscribe){

         evothings.ble.readDescriptor(
                connectedDevice.device,
                descriptor,
                function(data)
                {
                    var cleanedData = String.fromCharCode.apply(null, new Uint8Array(data));
                    if (cleanedData == sensor_type){
                            subscribe();
                    }
                },
                function(errorCode)
                {
                    console.log('readDescriptor error: ' + errorCode);
                });

   }

   // Get Data enables notifications from a characteristic
   function getData(){

        // If the sensor type is Accelerometer, then we will receive three data points
        if(sensor_type == "ACCELEROMETER"){
            evothings.ble.enableNotification(
                    connectedDevice.device,
                    connectedDevice.characteristic,
                    function(data)
                    {
                         //var buff = new Uint8Array(data);
                         //var cleanedData = buff[0];
                         //var result = new Float32Array(data);
                         var result = {};
                         result.X = new DataView(data).getFloat32(0, true);
                         result.Y = new DataView(data).getFloat32(4, true);
                         result.Z = new DataView(data).getFloat32(8, true);
                         result.timeStamp = new Date().getTime();
                         successCallback(result);


                    },
                    function(errorCode)
                    {
                        console.log('enableNotification error: ' + errorCode);
                        errorCallback();
                    });
     }

     // Otherwise we only receive one data point
     else{
            evothings.ble.enableNotification(
                    connectedDevice.device,
                    connectedDevice.characteristic,
                    function(data)
                    {
                         //var buff = new Uint8Array(data);
                         //var cleanedData = buff[0];
                         //var result = new Float32Array(data);
                         var result = {};
                         result.data = new DataView(data).getFloat32(0, true);
                         result.timeStamp = new Date().getTime();
                         successCallback(result);


                    },
                    function(errorCode)
                    {
                        console.log('enableNotification error: ' + errorCode);
                        errorCallback();
                    });

     }

   }

};
