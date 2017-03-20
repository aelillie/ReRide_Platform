/******************************************************************************
Flex_Sensor_Example.ino
Example sketch for SparkFun's flex sensors
  (https://www.sparkfun.com/products/10264)
Jim Lindblom @ SparkFun Electronics
April 28, 2016

Create a voltage divider circuit combining a flex sensor with a 47k resistor.
- The resistor should connect from A0 to GND.
- The flex sensor should connect from A0 to 3.3V
As the resistance of the flex sensor increases (meaning it's being bent), the
voltage at A0 should decrease.

Development environment specifics:
Arduino 1.6.7
******************************************************************************/
// Include BLE files.
#include <CurieBLE.h>

//###BLE Characteristics###
BLEPeripheral blePeripheral;       // BLE Peripheral Device (the board you're programming)
BLEService batteryService("180F"); // BLE Battery Service

// BLE Battery Level Characteristic
BLEUnsignedCharCharacteristic batteryLevelChar("2A19",  // standard 16-bit characteristic UUID
    BLERead | BLENotify);     // remote clients will be able to get notifications if this characteristic changes

//###Flex sensor properties###
const int FLEX_PIN = A0; // Pin connected to voltage divider output

// Measure the voltage at 5V and the actual resistance of your 47k resistor, and enter them below:
const float VCC = 4.98;       // Measured voltage of Ardunio 5V line
const float R_DIV = 47500.0;  // Measured resistance of 3.3k resistor

// Following can be adjusted to be more precise
const float STRAIGHT_RESISTANCE = 37300.0; // resistance when straight
const float BEND_RESISTANCE = 90000.0; // resistance at 90 deg

float oldFlexAngle = 0;  // last flex angle reading from analog input
long previousMillis = 0;  // last time the flex angle level was checked, in ms

void setup() 
{
  Serial.begin(9600);
  pinMode(FLEX_PIN, INPUT);

  /* Set a local name for the BLE device
     This name will appear in advertising packets
     and can be used by remote devices to identify this BLE device
     The name can be changed but maybe be truncated based on space left in advertisement packet */
  blePeripheral.setLocalName("ReRide");
  blePeripheral.setAdvertisedServiceUuid(batteryService.uuid());  // add the service UUID
  blePeripheral.addAttribute(batteryService);   // Add the BLE Battery service
  blePeripheral.addAttribute(batteryLevelChar); // add the battery level characteristic
  batteryLevelChar.setValue(oldFlexAngle);   // initial value for this characteristic

  /* Now activate the BLE device.  It will start continuously transmitting BLE
     advertising packets and will be visible to remote BLE central devices
     until it receives a new connection */
  blePeripheral.begin();
  Serial.println("Bluetooth device active, waiting for connections...");
}


// This function is called continuously, after setup() completes.
void loop() 
{
  // listen for BLE peripherals to connect:
  BLECentral central = blePeripheral.central();

  // if a central is connected to peripheral:
  if (central) {
    Serial.print("Connected to central: ");
    // print the central's MAC address:
    Serial.println(central.address());
    // check the flex angle every 200ms as long as the central is still connected:
    while (central.connected()) {
      long currentMillis = millis();
      // if 200ms have passed, check the flex angle:
      if (currentMillis - previousMillis >= 200) {
        previousMillis = currentMillis;
        updateFlexAngle();
      }
    }
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}

void updateFlexAngle() {
  // Read the ADC, and calculate voltage and resistance from it
  int flexADC = analogRead(FLEX_PIN);
  float flexV = flexADC * VCC / 1023.0;
  float flexR = R_DIV * (VCC / flexV - 1.0);

  // Use the calculated resistance to estimate the sensor's bend angle:
  float angle = map(flexR, STRAIGHT_RESISTANCE, BEND_RESISTANCE, 0, 90.0);

  if (angle != oldFlexAngle) {
    Serial.println("Resistance: " + String(flexR) + " ohms");
    Serial.println("Bend: " + String(angle) + " degrees");
    Serial.println();
    batteryLevelChar.setValue(angle);
    oldFlexAngle = angle;
  }
}

