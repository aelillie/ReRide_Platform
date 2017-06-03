console.log('Loading function');

var AWS = require('aws-sdk');
var lambda = new AWS.Lambda({
  region: 'eu-central-1' //change to your region
});

exports.handler = (event, context, callback) => {
    var payload_data = JSON.stringify(event.args, null, 2);
    console.log('Received event:', payload_data);
    
    lambda.invoke({
        FunctionName: 'ReRide_DataFetch',
        Payload: payload_data
    }, function(error, data) {
        if (error) {
            console.log('Lambda invocation failed');
            //context.done('error', error);
            callback(error, null);
        }
        if(data.Payload){
            console.log('Lambda invocation succeeded');
            //context.succeed(data.Payload);
            console.log('Received data:', data.Payload);
            var newItem = accumulate(data.Payload);
            console.log('New item: ', newItem);
            callback(null, newItem);
        }
    });
    
};

function accumulate(data) {
    var queryData = JSON.parse(data);//data.Items[0];
    var items = queryData.Items;
    var size = items.length;
    var sampleItem = items[0].payload;
    //console.log('Sampleitem: ', sampleItem);
    var lastItem = items[size-1].payload;
    
    var id = sampleItem.id;
    var startLon = sampleItem.longitude;
    var startLat = sampleItem.latitude;
    var startTime = sampleItem.time;
    var endLon = lastItem.longitude;
    var endLat = lastItem.latitude;
    var endTime = lastItem.time;
    
    var sensorValues = [];
    console.log('Sensor values: ', sensorValues);
    
    
    items.forEach(function(item, i, array) {
        //console.log('Payload ' + i, item);
        item.payload.sensors.forEach(function(sensor, j, array) {
            //console.log('Sensor ' + j, sensor);
            var sensorVal = parseInt(sensor.value);
            console.log('Sensorval: ', sensorVal);
            if (j < sensorValues.length) {
                var oldVal = sensorValues[j]/size;
                console.log('Oldval: ', oldVal);
                sensorValues[j] = oldVal + sensorVal;
                console.log('Val at ' + j + ': ', sensorValues[j]);
            } else {
                sensorValues[j] = sensorVal;
                console.log('Val at ' + j + ': ', sensorValues[j]);
            }
        });
    });
    
    var accData = [];
    
    for (i=0; i<sensorValues.length;i++) {
        var sampleSensor = sampleItem.sensors[i];
        var newItem = {
            id: id,
            start: {
                lon: startLon,
                lat: startLat,
                time: startTime
            },
            end: {
                lon: endLon,
                lat: endLat,
                time: endTime
            },
            sensors: [
                {
                    name: sampleSensor.name,
                    unit: sampleSensor.unit,
                    characteristic: sampleSensor.characteristic,
                    value: sensorValues[i]
                }
            ]
        };
        accData.push(newItem);
    }
    return accData;
}