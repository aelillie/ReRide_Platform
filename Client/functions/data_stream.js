var AWS = require('aws-sdk');
var dynamodbstreams = new AWS.DynamoDBStreams(
    {
        region: 'eu-central-1',
        accessKeyId: 'AKIAI4YALEWL6RNVOW6Q', //User: StreamUser
        secretAccessKey: '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'
    }
);

var streamArn;
var shardId;
var shardIterator;
const UPDATE_FREQ = 1; //seconds

function stream(timeValue) {
    let itemLimit = timeValue * 60 / UPDATE_FREQ;

    var streamParams = {
        TableName: 'ReRide_DDB'
    }
    dynamodbstreams.listStreams(streamParams, function(err, data) {
        if (err) console.log(err, err.stack); // an error occurred
        else {
            console.log(data); // successful response
            var streams = JSON.parse(data);
        }
    })

    var shardParams = {
        ShardId: shardId,
        ShardIteratorType: TRIM_HORIZON,
        StreamArn: streamArn        
    }

    dynamodbstreams.getShardIterator(shardParams, function(err, data) {
        if (err) console.log(err, err.stack); // an error occurred
        else {
            console.log(data); // successful response
            shardIterator = data;
        }
    })

    getRecords();
}

function getRecords() {
    while (shardIterator) {
            var recordParams = {
            ShardIterator: shardIterator,
            Limit: itemLimit
        }
        dynamodbstreams.getRecords(recordParams, function(err, recordsObj) {
            if (err) console.log(err, err.stack); // an error occurred
            else {
                console.log(recordsObj); // successful response
                var records = JSON.parse(recordsObj);
                shardIterator = records.NextShardIterator;
                var data = records.Records;
                handleData(data);
            }
        })
    }
}

function handleData(data) {
    var info = data.dynamodb;
    //TODO: Info is in here
}