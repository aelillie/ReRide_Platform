# ReRide_Project
Repository for my IoT Bachelor project 2017 in collaboration with the ReRide project. This is a simple Read-Me, which explains what the repository contains.

## Central
This folder contains projects for building the <i>Central</i>-role in the BLE relationship between the Smart Phone Gateway device and <i>Peripheral</i> devices.
### Android
This is the working project, and contains a rather simple app. It requires an Android BLE and Internet connected device with minimum API level 21. It supports the following GATT characteristics: Battery Level (2a19), Apparent Wind direction(2a73), Age(2a80), Weight(2a98), and Heart Rate Measurement(2a37). Launching the app, the following steps sets up the phone as Central and Gateway to AWS:
<ol>
  <li>From the start page you can select "Start ride" to scan for devices, connect and launch reading data as well as publishing it to AWS. This will end in a screen providing visualization of the live data.</li>
  <li>Connection is established in a background Service, which takes care of contionously reading data from connected peripherals, bundling it with GPS coordinates and a timestamp, and finally publishing them to AWS through MQTT in a JSON format. At this point the Central is up and working.</li>
  <li>Another service transmits live data through Bluetooth to an external device, which is supposed to visualize the data.</li>
  <li>Another screen presented by clicking "See historical data" will give the user the ability to poll information from AWS in a given time interval from the present time. </li>
</ol>

## Peripheral
Contains code for a Genuino (Arduino 101) device, publishing data through BLE as the peripheral role. Flex_sensor.ino is used as the primary test client. Environmental Sensing is used as the GATT service, as this contains the Apparent Wind Direction characteristic, which exactly allows integers represented as degress between 0 and 360, which is compliant with the flex sensor used throughout the project.

## Data
JSON formats used in all components throughout the system flow.

## AWS API
The API exposes a GET-method on the following endpoint: https://rzx2umf8a9.execute-api.eu-central-1.amazonaws.com/beta/ride-data.
The method request requires the following parameters:
<ol>
<li>"id": The <i>unique</i> id of the user, used to login in the Android app</li>
<li>"from": The number of minutes counting from present that data is wanted from.</li>
<li>"to": The number of minutes counting from present that data is wanted to.</li>
<li>"timezone": The client's timezone</li>
</ol>

If only the latest record is wanted, pass 0 as the argument to "from". If all data for the specified user is wanted, pass -1 as the argument to "from".
Furthermore an API key is required in the request header, which is provided for the client.
The response results in the query data in a JSON, following the schema providid in the data files (see above).
