#include <CurieBLE.h>

BLEPeripheral blePeripheral;
BLEService pressureSensorService = BLEService ("181C");

BLEUnsignedCharCharacteristic sensorOneReadingChar("2A80", BLERead | BLENotify); //Age

BLEUnsignedCharCharacteristic sensorTwoReadingChar("2A98", BLERead | BLENotify); //Weight

//int fakeSensorValue = 0;
int sensorOne, sensorTwo;
String test, test1, test2;

void setup() {
  Serial.begin(9600);
  blePeripheral.setLocalName("ReRide");
  blePeripheral.setAdvertisedServiceUuid(pressureSensorService.uuid());
  blePeripheral.addAttribute(pressureSensorService);
  blePeripheral.addAttribute(sensorOneReadingChar);
  blePeripheral.addAttribute(sensorTwoReadingChar);
  blePeripheral.begin();
  Serial.println("Bluetooth device active, waiting for connections...");
}

void loop()
{

  BLECentral central = blePeripheral.central();

  if (central) {
    Serial.print("Connected to central: ");

    Serial.println(central.address());

    sensorOne = 25;//analogRead(0);
    sensorTwo = 50;//analogRead(1);
    /*  test = String("Time :" , millis());
      test1 = String("Sensor 1 :" , currentState);
      test2 = String("Sensor 2 :" , fakeSensorValue);*/
    Serial.print("Time");
    Serial.println(millis());

    Serial.print("Sensor 1 : ");
    //Serial.println(map(sensorOne, 800, 1023, 0, 1023));

    Serial.print("Sensor 2 : ");
    //Serial.println(map(sensorTwo, 800, 1023, 0, 1023));

    //Serial.println(test);
    //Serial.println(test1);
    //Serial.println(test2);
    sensorTwoReadingChar.setValue(sensorTwo);
    sensorOneReadingChar.setValue(sensorOne);
    //fakeSensorValue++;
    delay(1000);
  }
}
