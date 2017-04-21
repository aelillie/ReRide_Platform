console.log('Loading function');

var AWS = require('aws-sdk');
var DynamoDB = new AWS.DynamoDB.DocumentClient();

exports.handler = function(event, context, callback) {
    console.log('Received event:', JSON.stringify(event, null, 2));

    var id = event.id;
    var since = parseInt(event.since);
    var tableName = 'ReRide_DDB';
    var now = Date.now();

    var startTime = new Date((now+7200000) - (since*60000)); //TODO: Automatically account for timezone
    var startTimeString =
        startTime.getHours() + ""
        + startTime.getMinutes() + ""
        + startTime.getSeconds();
    console.log('Start time:', startTimeString);

    var params;
    switch (since) {
        case -1: //Fetch all records
            params = {
                TableName: tableName,
                KeyConditionExpression: "ThingID = :i",
                ExpressionAttributeValues: { ":i": id }
            }; break;
        case 0:
            params = { //Fetch latest record
                TableName: tableName,
                KeyConditionExpression: "ThingID = :i",
                ExpressionAttributeValues: { ":i": id },
                ScanIndexForward: false,
                Limit: 1
            }; break;
        default:
            params = { //Fetch latest records within a time limit
                TableName: tableName,
                KeyConditionExpression: "ThingID = :i AND #T > :t",
                ExpressionAttributeValues: {
                    ":i": id,
                    ":t": startTimeString
                },
                ExpressionAttributeNames: {
                    "#T": 'Time'
                },
                ConsistentRead: true
            }; break;
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