console.log('Loading function');

var AWS = require('aws-sdk');
var DynamoDB = new AWS.DynamoDB.DocumentClient();

exports.handler = function(event, context, callback) {
    console.log('Received event:', JSON.stringify(event, null, 2));

    let id = event.id;
    let since = parseInt(event.since);
    var now = Date.now();

    var startTime = new Date((now+7200000) - (since*60000));
    var startTimeString =
        startTime.getHours() + ""
        + startTime.getMinutes() + ""
        + startTime.getSeconds();
    //console.log('Now:', new Date(now).toString());
    console.log('Start time:', startTimeString);
    //console.log('Difference:', (parseInt("12:35:00") > parseInt(startTimeString)));

    var params = {
        TableName: 'ReRide_DDB',
        KeyConditionExpression: "ThingID = :i AND #T > :t",
        ExpressionAttributeValues: {
            ":i": id,
            ":t": startTimeString
        },
        ExpressionAttributeNames: {
            "#T": 'Time'
        },
        ConsistentRead: true
    }

    DynamoDB.query(params, function(err, data) {
        if (err) {
            console.log(err, err.stack); // an error occurred
            callback(err, null);
        } else {
            console.log(data);           // successful response
            callback(null, data);
        }
    })
};