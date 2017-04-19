console.log('Loading function');

var AWS = require('aws-sdk');
var DynamoDB = new AWS.DynamoDB.DocumentClient();

exports.handler = function(event, context, callback) {
    console.log('Received event:', JSON.stringify(event, null, 2));

    let id = event.id;
    let since = parseInt(event.since);

    var startTime = new Date(
        Date.now() - (since*60000) //min to ms
        );
    var startTimeString =
          startTime.getHours()   + ":"
        + startTime.getMinutes() + ":"
        + startTime.getSeconds();

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