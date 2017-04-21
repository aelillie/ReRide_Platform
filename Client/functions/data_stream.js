var AWS = require('aws-sdk');
var dynamodbstreams = new AWS.DynamoDBStreams(
    {
        region: 'eu-central-1',
        accessKeyId: 'AKIAI4YALEWL6RNVOW6Q', //User: StreamUser
        secretAccessKey: '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'
    }
);

var streamArn;
var shards;
var shardId;
const UPDATE_FREQ = 1; //seconds

function stream(timeValue) {
    var items = timeValue * 60 / UPDATE_FREQ;
    var itemLimit = items > 1000 ? 1000 : items;

    dynamodbstreams.listStreams({TableName: 'ReRide_DDB'}, function(err, data) {
        if (err) console.log(err, err.stack); // an error occurred
        else {
            console.log(data); // successful response
            var streams = JSON.parse(data);
            if (streams.Streams.length > 1) {
                console.log('Received multiple streams');
                return;
            }
            streamArn = streams.LastEvaluatedStreamArn ?
                streams.LastEvaluatedStreamArn :
                streams.Streams.StreamArn;
        }
    });

    dynamodbstreams.describeStream({StreamArn: streamArn}, function (err, data) {
        if (err) console.log(err, err.stack); // an error occurred
        else {
            var streamInfo = JSON.parse(data);
            if (streamInfo.LastEvaluatedShardId) {
                shardId = streamInfo.LastEvaluatedShardId;
            }
            shards = streamInfo.Shards;
        }
    });

    var shardParams = {
        ShardId: (shardId == null ? shards[0] : shardId),
        ShardIteratorType: TRIM_HORIZON,
        StreamArn: streamArn        
    };

    dynamodbstreams.getShardIterator(shardParams, function(err, data) {
        if (err) console.log(err, err.stack); // an error occurred
        else {
            console.log(data); // successful response
            getRecords(data, itemLimit);
        }
    });
}

function getRecords(shardIterator, itemLimit) {
    while (shardIterator) {
            var recordParams = {
            ShardIterator: shardIterator,
            Limit: itemLimit
        };
        dynamodbstreams.getRecords(recordParams, function(err, recordsObj) {
            if (err) console.log(err, err.stack); // an error occurred
            else {
                console.log(recordsObj); // successful response
                var records = JSON.parse(recordsObj);
                shardIterator = records.NextShardIterator;
                handleData(records.Records);
            }
        })
    }
}

function handleData(data) {
    data.forEach(function (item) {
        var info = data.dynamodb.NewImage;
        var id = info.id;
        var angle = info.angle;
        var lat = info.latitude;
        var lon = info.longitude;
        var time = info.time;
    });
}