/**
 * Created by Anders on 22-Mar-17.
 */

var AWS = require('aws-sdk');
var AWSIoTData = require('aws-iot-device-sdk');
var AWSConfiguration = require('./aws-configuration.js');

console.log('Loaded AWS SDK for JavaScript and AWS IoT SDK for Node.js');

//
// Initialize our configuration.
//
AWS.config.region = AWSConfiguration.region;

AWS.config.credentials = new AWS.CognitoIdentityCredentials({
    IdentityPoolId: AWSConfiguration.poolId
});

//
// Keep track of whether or not we've registered the shadows used by this
// example.
//
var shadowsRegistered = false;

//
// Create the AWS IoT shadows object.  Note that the credentials must be
// initialized with empty strings; when we successfully authenticate to
// the Cognito Identity Pool, the credentials will be dynamically updated.
//
const shadows = AWSIoTData.thingShadow({
    //
    // Set the AWS region we will operate in.
    //
    region: AWS.config.region,
    //
    // Use a random client ID.
    //
    clientId: 'flex-sensor-publisher',
    //
    // Connect via secure WebSocket
    //
    protocol: 'wss',
    //
    // Set the maximum reconnect time to 8 seconds; this is a browser application
    // so we don't want to leave the user waiting too long for reconnection after
    // re-connecting to the network/re-opening their laptop/etc...
    //
    maximumReconnectTimeMs: 8000,
    //
    // Enable console debugging information (optional)
    //
    debug: true,
    //
    // IMPORTANT: the AWS access key ID, secret key, and sesion token must be
    // initialized with empty strings.
    //
    accessKeyId: '',
    secretKey: '',
    sessionToken: ''
});

//
// Update divs whenever we receive status events from the shadows.
//
shadows.on('status', function(name, statusType, clientToken, stateObject) {
    if (statusType === 'rejected') {
        //
        // If an operation is rejected it is likely due to a version conflict;
        // request the latest version so that we synchronize with the shadow
        // The most notable exception to this is if the thing shadow has not
        // yet been created or has been deleted.
        //
        if (stateObject.code !== 404) {
            console.log('resync with thing shadow');
            var opClientToken = shadows.get(name);
            if (opClientToken === null) {
                console.log('operation in progress');
            }
        }
    } else { // statusType === 'accepted'
        document.getElementById('flex_sensor-control').innerHTML = '<p>interior: ' + stateObject.state.desired.angle + '</p>';
    }
});

window.shadowConnectHandler = function() {
    console.log('connect');
    document.getElementById("connecting-div").style.visibility = 'hidden';
    document.getElementById("flex_sensor-control").style.visibility = 'visible';

    //
    // We only register our shadows once.
    //
    if (!shadowsRegistered) {
        shadows.register('FlexSensor', {
            persistentSubscribe: true
        });
        shadowsRegistered = true;
    }
    //
    // After connecting, wait for a few seconds and then ask for the
    // current state of the shadows.
    //
    setTimeout(function() {
        var opClientToken = shadows.get('FlexSensor');
        if (opClientToken === null) {
            console.log('operation in progress');
        }
    }, 3000);
};


//
// Reconnect handler; update div visibility.
//
window.shadowReconnectHandler = function() {
    console.log('reconnect');
    document.getElementById("connecting-div").style.visibility = 'visible';
    document.getElementById("flex_sensor-control").style.visibility = 'hidden';
};

//
// Install connect/reconnect event handlers.
//
shadows.on('connect', window.shadowConnectHandler);
shadows.on('reconnect', window.shadowReconnectHandler);

//
// Initialize divs.
//
document.getElementById('connecting-div').style.visibility = 'visible';
document.getElementById('flex_sensor-control').style.visibility = 'hidden';