# ReRide_Project
Repository for my IoT Bachelor project 2017 in collaboration with the ReRide project. This is a simple Read-Me, which explains what the repository contains.

## Central
This folder contains projects for building the <i>Central</i>-role in the BLE relationship between the Smart Phone Gateway device and <i>Peripheral</i> devices.
### Android
This is the working project, and contains a rather simple app. It requires an Android BLE and Internet connected device with minimum API level 21. At the moment it can only communicate with BLE peripherals publishing data though the Apparent Wind Direction GATT characteristics. Launching the app, the following steps sets up the phone as Central and Gateway to AWS:
<ol>
  <li>The start page is where you select and connect to the peripheral device(s)</li>
  <li>Connection is established in a background Service, which takes care of contionously reading data from connected peripherals, bundling it with GPS coordinates and a timestamp, and finally publishing them to AWS through MQTT in a JSON format. At this point the Central is up and working.</li>
	<li>Another service transmits live data through Bluetooth to an external device, which is supposed to visualize the data. <b>IN PROGRESS</b></li>
  <li>Although the app now fulfills its purpose a page for visualization of data is presented. This shows current data as well as polls historical data from AWS</li>
</ol>
### JavaScript
More of a test project. Was created in the beginning. As I am new to JS, i felt more comfortable going with Android, taking my deadline into consideration. This project does <i>not</i> work.

## Peripheral
Contains code for a Genuino (Arduino 101) device, publishing data through BLE as the peripheral role. Environmental Sensing is used as the GATT service, as this contains the Apparent Wind Direction characteristic, which exactly allows integers represented as degress between 0 and 360, which is compliant with the flex sensor used throughout the project.

## Data
JSON formats used in all components throughout the system flow.

## Client
In-progress client code for representing and visualizing data fetched from AWS. This is a test project and not part of scope.
