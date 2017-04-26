console.log('Loading function');

var AWS = require('aws-sdk');
var DynamoDB = new AWS.DynamoDB.DocumentClient();
const TABLENAME = 'ReRide_DDB';
const MS_PER_SEC = 1000;
const MS_PER_MIN = MS_PER_SEC*60;
const MS_PER_HOUR = MS_PER_MIN*60;

exports.handler = function(event, context, callback) {
    console.log('Received event:', JSON.stringify(event, null, 2));

    var id = event.id;
    var from = parseInt(event.from);
    var to = parseInt(event.to);
    var timeZone = parseInt(event.timeZone);
    console.log('Time zone: ', timeZone)
    
    var now = Date.now(); //ms since midnight Jan 1, 1970
    console.log('Now (ms): ', now);
    now = now+timeZone*MS_PER_HOUR; //Difference between UTC and local (mins)
    console.log('Now+zone (ms): ', now);

    //DEBUG
    var present = new Date(now);
    console.log('Now', present.toString());

    var fromTimeString = getTimeString(new Date(now - (from*MS_PER_MIN)));
    console.log('Start time:', fromTimeString);

    var toTimeString = getTimeString(new Date(now - (to*MS_PER_MIN)));
    console.log('End time:', toTimeString);

    var params;
    switch (from) {
        case -1: //Fetch all records
            params = {
                TableName: TABLENAME,
                KeyConditionExpression: "ThingID = :i",
                ExpressionAttributeValues: { ":i": id }
            }; break;
        case 0:
            params = { //Fetch latest record
                TableName: TABLENAME,
                KeyConditionExpression: "ThingID = :i",
                ExpressionAttributeValues: { ":i": id },
                ScanIndexForward: false,
                Limit: 1
            }; break;
        default:
            params = { //Fetch latest records within a time limit
                TableName: TABLENAME,
                KeyConditionExpression: "ThingID = :i AND #T BETWEEN :f AND :t",
                ExpressionAttributeValues: {
                    ":i": id,
                    ":f": fromTimeString,
                    ":t": toTimeString
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

function getTimeString(date) {
    var month = date.getMonth()+1;
    var day = date.getDate();
    var hours = date.getHours();
    var minutes = date.getMinutes();
    var seconds = date.getSeconds();

    return date.getFullYear()
         + format(month)       
         + format(day)     
         + format(hours)    
         + format(minutes)  
         + format(seconds);
}

function format(time) {
    return time < 10 ? "0"+time : ""+time
}