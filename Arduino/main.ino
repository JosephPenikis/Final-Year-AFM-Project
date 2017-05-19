#include "Arduino.h"

#include "MAX1168.h"
#include "DAC7574.h"
#include "LaserHelper.h"
#include <Wire.h>

#include "dac_control.h"

//#define TIMING_MODE

const int slaveSelectPin = 2;
const int EOCpin = 3;

MAX1168 adc(slaveSelectPin, EOCpin, RINT_ON_BUF_ON);
DAC7574 dac(A1_LOW_A0_LOW, I2C_STANDARD);

DAC piezoDAC;

void setup()
{
  //pinMode(13, OUTPUT);

  Serial.begin(115200);
  Serial.println("Hello... starting sampling!");

  dac.begin(DAC7574_UPDATE_NOW);
//  enableLaser();

  piezoDAC.init(12, 11);

  dac.writeToChannel(0x7ff, CHANNEL_A);
  dac.writeToChannel(0x7ff, CHANNEL_B);
  dac.writeToChannel(0x7ff, CHANNEL_C);
  
  SPI.begin();
}

uint8_t data[8 * sizeof(uint16_t)];

float voltages;

void adcTest() {
  #ifdef TIMING_MODE
    unsigned long start = millis();

    adc.beginTransaction();

    for (int i = 0; i < 10000; i++) {
      adc.readMultipleChannels_16bit(data, CHANNEL_START_0, 4);
    }

    adc.endTransaction();

    unsigned long end = millis();

    Serial.print("Average time per sample: ");
    Serial.print((float)(end - start) / 10000, 4);
    Serial.println("ms");
  #else
    digitalWrite(slaveSelectPin, LOW);

    adc.beginTransaction();

    #define N 4
    adc.readMultipleChannels_16bit(data, CHANNEL_START_4, N);

    adc.endTransaction();

    digitalWrite(slaveSelectPin, HIGH);

    for (int i = 0; i < N; i++)
    {
      voltages = (float)data[i] / 65536.0f * 4.096f;

      Serial.print(i); Serial.print(": ");
      Serial.print(voltages, 5);
      Serial.print("\t");
    }

    Serial.println();

    delay(50);
  #endif
}

bool newPiezoLocation = false;
bool scanningModeEnabled = false;

uint16_t zStep = 256;
int i;

int AVERAGES = 1000;
float THRESHOLD = 0.05f;

uint16_t voiceCoils[3];
uint16_t piezos[3]; // X \ Y \ Z

void updatePiezoXY() {
  // TODO: Complete with the SPI driver commands from Daniel and Bart
}

void updatePiezosZ() {
  piezoDAC.setValueZ((int32_t)piezos[2]);

  // Invalidate the data
  newPiezoLocation = false;
}

uint16_t value = 0;

void loop()
{
  adcTest();
  return;
  
  // Default control loop, just stream the DACa data
  if (!scanningModeEnabled) {
    digitalWrite(slaveSelectPin, LOW);

    adc.beginTransaction();

    #define N 4
    adc.readMultipleChannels_16bit(data, CHANNEL_START_4, N);

    adc.endTransaction();

    digitalWrite(slaveSelectPin, HIGH);

    Serial.write(0xff);

    for (uint16_t i = 0; i < (N * sizeof(uint16_t)); i++) {
      Serial.write(data[i]);
    }

    Serial.write(0xfe);

    delay(1);

    // 'Standard' z-movement function call
    if (newPiezoLocation) {
      updatePiezosZ();

      // Invalidate the data
      newPiezoLocation = false;
    }

  } else {
    // Starting a scan
    if (newPiezoLocation) {

      // Move the piezos to the desired X,Y coordinate
      updatePiezoXY();

      float averaging = 0.0f;
      float FES, sum, initialFES = -1.0f;

      // Run this loop 'forever' until condition met
      while(true) {
        digitalWrite(slaveSelectPin, LOW);
        adc.beginTransaction();

        for (int i = 0; i < AVERAGES; i++) {
          // Read the A,B,C,D segments
           adc.readMultipleChannels_16bit(data, CHANNEL_START_4, 4);

           // Compute the FES, TODO: Verify this sum!
           sum = data[0] + data[1] + data[2] + data[3];
           FES = ((data[3] - data[2]) + (data[1] - data[0])) / sum;

           averaging += FES;
        }

        adc.endTransaction();
        digitalWrite(slaveSelectPin, HIGH);

        averaging /= AVERAGES;

        // Log initial FES if beginning of new z-scan
        if (initialFES == -1.0f) {
          initialFES = averaging;
        } else{
          // Otherwise, compare to intitial!
          float diff = fabs(initialFES - averaging);

          if (diff > THRESHOLD) {
            delay(10); // Glitch fix
            // If above threshold, write data back to host and break!
            Serial.write(0xef);
            Serial.write(piezos[2]);

            // TODO: Temporary, redumentary sync fix to remove issue with varialbe packet sizes
            for (int i = 0; i < 6; i++) {
              Serial.write(0x00);
            }

            Serial.write(0xfe);

            piezos[2] = 0x00;

            updatePiezosZ();

            break;
          }
        }

        // Increment z
        piezos[2] += zStep;

        // TODO: Set cutoff properly and handle this occurance in software
        if (piezos[2] > 65000) {
          Serial.write(0xef);
          Serial.write(0xff);
          Serial.write(0xff);

          // TODO: Temporary, redumentary sync fix to remove issue with varialbe packet sizes
          for (int i = 0; i < 6; i++) {
            Serial.write(0x00);
          }

          Serial.write(0xfe);

          piezos[2] = 0x00;

          updatePiezosZ();

          break;

        }

        updatePiezosZ();
      }
    }
  }
}

String key = "6JcP8EmMlV02NqtWbfeC9rQSkotTCtenOfo6E9UIYbBwiwVc7ZGtRpj1s0MWeGg";
String laserOff = "PowerdownTheLaserImmediately";

void serialEvent() {
  // If enough potential data available
  if (Serial.available() >= 8) {
    // Needs to be start word
    if (Serial.read() != 0xff) return;

    int val = Serial.read();

    // 0xEF is a LASER control command
    if (val == 0xef) {
      String receive = Serial.readStringUntil('\n');

      if (receive == key)
        turnOnLaser();
      else if (receive == laserOff)
        turnOffLaser();

    // 0xFF is a Voice coil command
  } else if (val == 0xdf) {
      voiceCoils[0] = Serial.read() | (Serial.read() << 8);
      voiceCoils[1] = Serial.read() | (Serial.read() << 8);
      voiceCoils[2] = Serial.read() | (Serial.read() << 8);

      dac.writeToChannel(voiceCoils[0], CHANNEL_A);
      dac.writeToChannel(voiceCoils[1], CHANNEL_B);
      dac.writeToChannel(voiceCoils[2], CHANNEL_C);

    // X/Y command
    } else if (val == 0xcf) {
        piezos[0] = Serial.read() | (Serial.read() << 8); // x
        piezos[1] = Serial.read() | (Serial.read() << 8); // y
        piezos[2] = Serial.read() | (Serial.read() << 8); // z

        // if z is 0xffff then x-y scan mode packet, otherwise, just a manual update
        if (piezos[2] == 0xffff) {
          scanningModeEnabled = true;
        }
        
        newPiezoLocation = true;
    // Control word (averaging interval etc...)
    } else if (val == 0xbf) {

    }
    
    return;
  }
  
}

