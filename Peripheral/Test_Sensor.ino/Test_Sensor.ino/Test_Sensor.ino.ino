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
BLEService batteryService("180F"); // BLE "Battery service"

// BLE Battery Level Characteristic
BLEUnsignedCharCharacteristic batteryLevel("2A19",  // standard 16-bit characteristic UUID
    BLERead | BLENotify);     // remote clients will be able to get notifications if this characteristic changes

int value = 50;

void setup() 
{
  Serial.begin(9600);
  pinMode(13, OUTPUT); // initialize the LED on pin 13 to indicate when a central is connected
  while(!Serial);
  /* Set a local name for the BLE device
     This name will appear in advertising packets
     and can be used by remote devices to identify this BLE device
     The name can be changed but maybe be truncated based on space left in advertisement packet */
  blePeripheral.setLocalName("TestSensor1");
  blePeripheral.setAdvertisedServiceUuid(batteryService.uuid());  // add the service UUID
  blePeripheral.addAttribute(batteryService);   // Add the BLE Battery service
  blePeripheral.addAttribute(batteryLevel); // add the battery level characteristic
  batteryLevel.setValue(value);   // initial value for this characteristic
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
    digitalWrite(13, HIGH);
    while (central.connected()) {
      if (value == 100) {
        value = 0;
      } else {
        value = value + 1;
      }
      delay(500);
    }
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
    // when the central disconnects, turn off the LED:
    digitalWrite(13, LOW);
  }
}

