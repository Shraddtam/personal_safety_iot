#include <HardwareSerial.h>
#include <BluetoothSerial.h>

// GPS pin definitions
#define GPS_RX 16
#define GPS_TX 17

// Button and buzzer pin definitions
#define BUTTON_LOCATION 25
#define BUTTON_BUZZER 26
#define BUZZER_PIN 27

HardwareSerial GPS(1);  // Use UART1 for GPS
BluetoothSerial SerialBT; // Bluetooth serial instance

unsigned long startTime;
const unsigned long attemptTime = 40 * 1000; // 40 seconds per attempt
int currentAttempt = 1;
const int maxAttempts = 3; // Maximum 3 attempts
bool fixFound = false;
bool attemptComplete = false;
bool locationMode = false;
bool buzzerActive = false;

int locationPressCount = 0;
int buzzerPressCount = 0;

unsigned long lastLocationPressTime = 0;
unsigned long lastBuzzerPressTime = 0;
const unsigned long pressResetTime = 3000; // 3 seconds

String lastLatitude = "";
String lastLongitude = "";

// Bluetooth status tracking
bool btConnected = false;
unsigned long lastBtStatusCheck = 0;
const unsigned long btStatusInterval = 5000; // Check BT status every 5 seconds

void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(BUTTON_LOCATION, INPUT_PULLUP);
  pinMode(BUTTON_BUZZER, INPUT_PULLUP);
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);

  Serial.println("GPS tracking program starting...");

  SerialBT.begin("ESP32_GPS_Tracker"); // Bluetooth device name
  Serial.println("Bluetooth initialized. Connect to 'ESP32_GPS_Tracker' from your phone.");

  Serial.println("Press Location button 3 times to start GPS.");
  Serial.println("Press Buzzer button 3 times to activate buzzer.");
}

void loop() {
  checkBluetoothStatus();
  forwardBluetoothData();

  // Handle Location button
  if (digitalRead(BUTTON_LOCATION) == LOW) {
    if (millis() - lastLocationPressTime > 300) {
      locationPressCount++;
      Serial.print("Location button press: ");
      Serial.print(locationPressCount);
      Serial.println("/3");

      lastLocationPressTime = millis();
      if (locationPressCount >= 3 && !locationMode) {
        locationMode = true;
        Serial.println("Location button activated. Starting GPS...");
        GPS.begin(9600, SERIAL_8N1, GPS_RX, GPS_TX);
        currentAttempt = 1;
        startTime = millis();
        Serial.print("Starting GPS fix attempt ");
        Serial.print(currentAttempt);
        Serial.print(" of ");
        Serial.println(maxAttempts);
        attemptComplete = false;
        fixFound = false;
      }
    }
  }
  if (millis() - lastLocationPressTime > pressResetTime) locationPressCount = 0;

  // Handle Buzzer button
  if (digitalRead(BUTTON_BUZZER) == LOW) {
    if (millis() - lastBuzzerPressTime > 300) {
      buzzerPressCount++;
      Serial.print("Buzzer button press: ");
      Serial.print(buzzerPressCount);
      Serial.println("/3");

      lastBuzzerPressTime = millis();
      if (buzzerPressCount >= 3) {
        buzzerActive = true;
        Serial.println("Buzzer activated!");
        digitalWrite(BUZZER_PIN, HIGH);
        delay(2000);
        digitalWrite(BUZZER_PIN, LOW);
        buzzerPressCount = 0;
      }
    }
  }
  if (millis() - lastBuzzerPressTime > pressResetTime) buzzerPressCount = 0;

  if (locationMode && !fixFound) {
    if (!attemptComplete && (millis() - startTime > attemptTime)) {
      attemptComplete = true;
      Serial.println();
      Serial.print("Attempt ");
      Serial.print(currentAttempt);
      Serial.println(" timed out without finding a fix.");

      if (currentAttempt < maxAttempts) {
        currentAttempt++;
        Serial.print("Starting GPS fix attempt ");
        Serial.print(currentAttempt);
        Serial.print(" of ");
        Serial.println(maxAttempts);

        startTime = millis();
        attemptComplete = false;
      } else {
        Serial.println("All 3 attempts completed. No fix found.");

        if (lastLatitude != "" && lastLongitude != "") {
          Serial.println("Last known coordinates:");
          Serial.print("Latitude: "); Serial.println(lastLatitude);
          Serial.print("Longitude: "); Serial.println(lastLongitude);
        } else {
          Serial.println("No coordinate data available.");
        }

        Serial.println("Program ended after maximum attempts.");
        locationMode = false;
      }
    }

    if (!attemptComplete) {
      static unsigned long lastPrint = 0;
      if (millis() - lastPrint > 1000) {
        Serial.print(".");
        lastPrint = millis();
      }

      static unsigned long lastDebugPrint = 0;
      if (millis() - lastDebugPrint > 10000) {
        Serial.println();
        Serial.print("Attempt ");
        Serial.print(currentAttempt);
        Serial.print(" in progress (");
        Serial.print((millis() - startTime) / 1000);
        Serial.print(" seconds elapsed of ");
        Serial.print(attemptTime / 1000);
        Serial.println(" seconds)");
        lastDebugPrint = millis();
      }

      while (GPS.available()) {
        String line = GPS.readStringUntil('\n');
        line.trim();
        Serial.println(line);

        if (line.startsWith("$GPRMC")) {
          String parts[15]; int idx = 0, start = 0;
          for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ',' || i == line.length() - 1) {
              parts[idx++] = line.substring(start, i);
              start = i + 1;
              if (idx >= 15) break;
            }
          }
          if (idx > 2 && parts[2] == "A" && idx > 5) {
            fixFound = true;
            lastLatitude = convertToDecimalDegrees(parts[3], parts[4]);
            lastLongitude = convertToDecimalDegrees(parts[5], parts[6]);
            Serial.println("GPS Fix Found!");

            printCoordinates(lastLatitude, lastLongitude);
            sendCoordinatesBT(lastLatitude, lastLongitude);

            digitalWrite(BUZZER_PIN, HIGH);
            delay(500);
            digitalWrite(BUZZER_PIN, LOW);
            locationMode = false;
          }
        } else if (line.startsWith("$GPGGA")) {
          String parts[15]; int idx = 0, start = 0;
          for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ',' || i == line.length() - 1) {
              parts[idx++] = line.substring(start, i);
              start = i + 1;
              if (idx >= 15) break;
            }
          }
          if (idx > 6 && parts[6] != "0") {
            fixFound = true;
            lastLatitude = convertToDecimalDegrees(parts[2], parts[3]);
            lastLongitude = convertToDecimalDegrees(parts[4], parts[5]);
            Serial.println("GPS Fix Found!");

            printCoordinates(lastLatitude, lastLongitude);
            sendCoordinatesBT(lastLatitude, lastLongitude);

            digitalWrite(BUZZER_PIN, HIGH);
            delay(500);
            digitalWrite(BUZZER_PIN, LOW);
            locationMode = false;
          } else if (idx > 5 && parts[2].length() > 0 && parts[4].length() > 0) {
            lastLatitude = convertToDecimalDegrees(parts[2], parts[3]);
            lastLongitude = convertToDecimalDegrees(parts[4], parts[5]);
          }
        }
      }
    }
  }
}

void checkBluetoothStatus() {
  if (millis() - lastBtStatusCheck > btStatusInterval) {
    bool currentStatus = SerialBT.connected();
    if (currentStatus != btConnected) {
      btConnected = currentStatus;
      if (btConnected) {
        Serial.println("Bluetooth device connected!");
        SerialBT.println("ESP32 GPS Tracker connected successfully!");
      } else {
        Serial.println("Bluetooth device disconnected!");
      }
    }
    lastBtStatusCheck = millis();
  }
}

void forwardBluetoothData() {
  if (SerialBT.available()) {
    String receivedMsg = SerialBT.readStringUntil('\n');
    receivedMsg.trim();
    if (receivedMsg.length() > 0) {
      Serial.print("BT received: ");
      Serial.println(receivedMsg);
      SerialBT.println("Received: " + receivedMsg);
    }
  }
}

String convertToDecimalDegrees(String coord, String dir) {
  if (coord.length() < 1) return "";
  float value = coord.toFloat();
  int degreesPart;

  degreesPart = int(value / 100);
  float minutesPart = value - (degreesPart * 100);
  value = degreesPart + (minutesPart / 60.0);

  if (dir == "S" || dir == "W") value = -value;

  return String(value, 6) + "° " + dir;
}

String getDecimalOnly(String coordWithDir) {
  int degreeIndex = coordWithDir.indexOf("°");
  if (degreeIndex != -1) {
    return coordWithDir.substring(0, degreeIndex);
  }
  return coordWithDir;
}

void sendCoordinatesBT(String lat, String lon) {
  if (SerialBT.connected()) {
    String cleanLat = getDecimalOnly(lat);
    String cleanLon = getDecimalOnly(lon);
    String message = "COORDS:" + cleanLat + "," + cleanLon;
    SerialBT.println(message);
  }
}

void printCoordinates(String lat, String lon) {
  Serial.println("Coordinates:");
  Serial.print("Latitude: "); Serial.println(lat);
  Serial.print("Longitude: "); Serial.println(lon);
}
