# ReRide_Platform
Repository for my IoT Bachelor project 2017 in collaboration with the ReRide team. This is a simple Read-Me, which explains what the repository contains.

## GatewayCentral
This folder contains the "ReRide" Android application, an example data record, and an installable APK of the application.
### Installing the APK
With Android Debug Bridge (adb) available (https://developer.android.com/studio/command-line/adb.html#move), locate the .apk file in a shell and execute "adb install <path-to-apk>" with either an emulator device or physical device connected to the computer.
### ReRide
The project creates the Gateway/Central application. It requires an Android BLE and Internet connected device with minimum API level 21. It supports the following GATT characteristics: Battery Level (2a19), Apparent Wind direction(2a73), Age(2a80), Weight(2a98), and Heart Rate Measurement(2a37). 

Launching the app, the following steps sets up the phone as BLE Central and Gateway to AWS:
<ol>
  <li>From the start page you can select "Start ride" to scan for devices, connect and launch reading data as well as publishing it to AWS. This will end in a screen providing visualization of the live data. Connection is established in a background Service, which takes care of contionously reading data from connected peripherals, bundling it with GPS coordinates and a timestamp, and finally publishing them to AWS through MQTT in a JSON format.</li>
  <li>Another screen presented by clicking "See historical data" from the start page will give the user the ability to poll information from AWS in a given time interval from the present time. It will use the user id used as login to poll information.</li>
  <li>"Multi-Ride" is not implemented yet.</li>
  <li>Change settings allows the user to change login id.
</ol>

## Peripheral
Contains code for a Genuino (Arduino 101) device, publishing data through BLE as the peripheral role. Flex_sensor.ino is used as the primary test client. Environmental Sensing is used as the GATT service, as this contains the Apparent Wind Direction characteristic, which exactly allows integers represented as degress between 0 and 360, which is compliant with the flex sensor used throughout the project.

## Cloud
### API Gateway
The API exposes a GET-method on the following endpoint: https://rzx2umf8a9.execute-api.eu-central-1.amazonaws.com/beta/ride-data.
The method request requires the following parameters:
<ol>
<li>"id": The <i>unique</i> id of the user, used to login in the Android app</li>
<li>"from": The number of minutes counting from present that data is wanted from.</li>
<li>"to": The number of minutes counting from present that data is wanted to.</li>
<li>"timezone": The client's timezone</li>
</ol>

Example request: https://rzx2umf8a9.execute-api.eu-central-1.amazonaws.com/beta/ride-data?id=1234&from=120&to=60&timezone=2. This fetches data for user "1234" in the time period from 2 hours ago until 1 hour ago in the Central Europe timezone.

If only the latest record is wanted, pass 0 as the argument to "from". If all data for the specified user is wanted, pass -1 as the argument to "from".
Furthermore an API key is required in the request header for the key "x-api-key", which is provided for the client.
The response results in the query data in a JSON, following the schema providid in "ReRideDataModel.json".

### Lambda
ReRide_DataFetch.js is the function deployed in AWS Lambda and is invoked with each API call and executed in the Cloud. The function queries the DynamoDB table with parameters based on the input from the API call.