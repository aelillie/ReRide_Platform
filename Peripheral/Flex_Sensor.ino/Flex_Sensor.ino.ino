/******************************************************************************
Source: https://learn.sparkfun.com/tutorials/flex-sensor-hookup-guide
Source: https://www.arduino.cc/en/Reference/CurieBLE

Create a voltage divider circuit combining a flex sensor with a 47k resistor.
- The resistor should connect from A0 to GND.
- The flex sensor should connect from A0 to 3.3V
As the resistance of the flex sensor increases (meaning it's being bent), the
voltage at A0 should decrease.
Transmit to BLE centrals
******************************************************************************/
// Include BLE files.
#include <CurieBLE.h>

//###BLE Characteristics###
BLEPeripheral blePeripheral;       // BLE Peripheral Device (the board you're programming)
BLEService sensingService("181A"); // BLE "Environmental Sensing"

// BLE Battery Level Characteristic
BLEUnsignedIntCharacteristic windDirChar("2A73",  // standard 16-bit characteristic UUID
    BLERead | BLENotify);     // remote clients will be able to get notifications if this characteristic changes

//###Flex sensor properties###
const int FLEX_PIN = A0; // Pin connected to voltage divider output
const int LED_PIN = 13;

// Measure the voltage at 5V and the actual resistance of your 47k resistor, and enter them below:
const float VCC = 4.98;       // Measured voltage of Ardunio 5V line
const float R_DIV = 47500.0;  // Measured resistance of 3.3k resistor

// Following can be adjusted to be more precise
const float STRAIGHT_RESISTANCE = 13000.0; // resistance when straight
const float BEND_RESISTANCE = 20000.0; // resistance at 90 deg

int oldFlexAngle = 0;  // last flex angle reading from analog input
long previousMillis = 0;  // last time the flex angle level was checked, in ms

void setup() 
{
  Serial.begin(9600);
  pinMode(FLEX_PIN, INPUT); // initialize serial communication
  pinMode(LED_PIN, OUTPUT); // initialize the LED on pin 13 to indicate when a central is connected
  Serial.println("Attached flex pin");
  /* Set a local name for the BLE device
     This name will appear in advertising packets
     and can be used by remote devices to identify this BLE device
     The name can be changed but maybe be truncated based on space left in advertisement packet */
  blePeripheral.setLocalName("FlexSensor1");
  blePeripheral.setAdvertisedServiceUuid(sensingService.uuid());  // add the service UUID
  blePeripheral.addAttribute(sensingService);   // Add the BLE Battery service
  blePeripheral.addAttribute(windDirChar); // add the battery level characteristic
  windDirChar.setValue(oldFlexAngle);   // initial value for this characteristic
  while(!Serial);
  Serial.println("Configured BLE");
  /* Now activate the BLE device.  It will start continuously transmitting BLE
     advertising packets and will be visible to remote BLE central devices
     until it receives a new connection */
  blePeripheral.begin();
  Serial.println("Bluetooth device active, waiting for connections...");
}


// This function is called continuously, after setup() completes.
void loop() 
{
  // listen for BLE centrals to connect:
  BLECentral central = blePeripheral.central();
 
  // if a central is connected to peripheral:
  if (central) {
    Serial.print("Connected to central: ");
    // print the central's MAC address:
    Serial.println(central.address());
    // turn on the LED to indicate the connection:
    digitalWrite(LED_PIN, HIGH);
    // check the flex angle every 200ms as long as the central is still connected:
    while (central.connected()) {
      updateFlexAngle();
      delay(500);
      /*long currentMillis = millis();
      // if 200ms have passed, check the flex angle:
      if (currentMillis - previousMillis >= 200) {
        previousMillis = currentMillis;
        updateFlexAngle();
      }*/
    }
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
    // when the central disconnects, turn off the LED:
    digitalWrite(LED_PIN, LOW);
  }
}

void updateFlexAngle() {
  // Read the ADC, and calculate voltage and resistance from it
  int flexADC = analogRead(FLEX_PIN);
  float flexV = flexADC * VCC / 1023.0;
  float flexR = R_DIV * (VCC / flexV - 1.0);

  // Use the calculated resistance to estimate the sensor's bend angle:
  int angle = (int) map(flexR, STRAIGHT_RESISTANCE, BEND_RESISTANCE, 0, 90.0);
  uint16_t uangle = (uint16_t) abs(angle);
  if (angle != oldFlexAngle) {
    Serial.println("Resistance: " + String(flexR) + " ohms");
    Serial.println("Bend: " + String(uangle) + " degrees");
    Serial.println();
    windDirChar.setValue(uangle);
    oldFlexAngle = angle;
  }
}

