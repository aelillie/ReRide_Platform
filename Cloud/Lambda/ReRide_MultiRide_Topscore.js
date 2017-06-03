console.log('Loading function');

var AWS = require('aws-sdk');
var lambda = new AWS.Lambda({
  region: 'eu-central-1' //change to your region
});
var DynamoDB = new AWS.DynamoDB.DocumentClient();
const TABLENAME_MULTIRIDE = "MultiRide_DDB";
const TABLENAME_RERIDE = "ReRide_DDB";
var groupID;

exports.handler = (event, context, callback) => {
    console.log('Received event:', JSON.stringify(event, null, 2));

    var id = event.userID;
    console.log('User id: ', id);
    
    DynamoDB.query({
            TableName: TABLENAME_MULTIRIDE,
            KeyConditionExpression: "UserID = :i",
            ExpressionAttributeValues: { ":i": id }
        }, function(err, data) {
            if (err) {
                console.log(err, err.stack); // an error occurred
                callback(err, null);
            } else {
                console.log(data);           // successful response
                groupID = data.Items[0].GroupID;
                console.log('GroupID: ', groupID);
            }
        }
    );
    
    DynamoDB.query({
            TableName: 'MultiRide_Lookup_DDB',
            KeyConditionExpression: "GroupID = :i",
            ExpressionAttributeValues: { ":i": groupID }
        }, function(err, data) {
            if (err) {
                console.log(err, err.stack); // an error occurred
                callback(err, null);
            } else {
                console.log(data);           // successful response
                callback(null, getHighscore(data));
            }
        }
    );
    
};

function getHighscore(users) {
    var scores = [];
    users.forEach(function(item) {
        lambda.invoke({
            FunctionName: 'ReRide_Accumulate',
            Payload: {
                "id": item,
                "from": "1440",
                "to": "0",
                "timezone": "2" //TODO: Custom
            }}, 
            function(error, data) {
                if (error) {
                    console.log('Lambda invocation failed');
                    //context.done('error', error);
                    callback(error, null);
                } else {
                    console.log('Lambda invocation succeeded');
                    console.log('Received data:', data);
                    scores.push(data);
                }
            }
        );
    });
    return calculate(scores);
}

function calculate(scores) {
    var maxScore;
    var winner;
    scores.forEach(function(score) {
        var midVal;
        score.sensors.forEach(function(sensor) {
            midVal += sensor.value;
        });
        if (midVal > maxScore) {
            maxScore = midVal;
            winner = score;
        }
    });
    return winner;
}