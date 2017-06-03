console.log('Loading function');

var AWS = require('aws-sdk');
var DynamoDB = new AWS.DynamoDB.DocumentClient();
const TABLENAME = 'MultiRide_DDB';

exports.handler = (event, context, callback) => {
    console.log('Received event:', JSON.stringify(event, null, 2));

    var userID = event.userID;
    console.log('UserID:', userID);
    var groupID = event.groupID;
    console.log('GroupID:', groupID);
    
    
    DynamoDB.put({
        Item: {
            "UserID": userID,
            "GroupID": groupID
        },
        TableName: TABLENAME,
        ReturnConsumedCapacity: "TOTAL"
        }, function(err, data) {
            if (err) {
                console.log(err, err.stack);
                callback(err, null);
            } else {
                console.log(data);
                callback(null, data);
            }
        }
    );
    
    DynamoDB.put({
        Item: {
            "GroupID": groupID,
            "UserID": userID
        },
        TableName: 'MultiRide_Lookup_DDB',
        ReturnConsumedCapacity: "TOTAL"
        }, function(err, data) {
            if (err) {
                console.log(err, err.stack);
                callback(err, null);
            } else {
                console.log(data);
                callback(null, data);
            }
        }
    );
    
};