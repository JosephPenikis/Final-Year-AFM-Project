#include "Arduino.h"

#define LASER_CURRENT_EN  A6
#define LASER_EN          A7
#define LASER_OUTSEL      A1
#define LASER_ENBL        A2
#define LASER_OSCEN       A4
#define LASER_WENRB       A5

bool laserEnabled = false;
bool laserDriverEnabled = false;

void powerOnLaserDriver()
{
  // TODO: Prevent this running on each instance
  pinMode(LASER_CURRENT_EN, OUTPUT);
  pinMode(LASER_EN, OUTPUT);

  if (laserDriverEnabled) return;

  // Start with all control lines as floating/high-impedence
  pinMode(LASER_WENRB, INPUT);
  pinMode(LASER_ENBL, INPUT);
  pinMode(LASER_OSCEN, INPUT);
  pinMode(LASER_OUTSEL, INPUT);

  // Turn on the devices power
  digitalWrite(LASER_EN, HIGH);

  // Now set the control lines as outputs
  pinMode(LASER_WENRB, OUTPUT);
  pinMode(LASER_ENBL, OUTPUT);
  pinMode(LASER_OSCEN, OUTPUT);
  pinMode(LASER_OUTSEL, OUTPUT);

  delay(1);

  digitalWrite(LASER_WENRB, HIGH);
  digitalWrite(LASER_ENBL, LOW);
  digitalWrite(LASER_CURRENT_EN, LOW);
  digitalWrite(LASER_OSCEN, LOW);

  delay(1);

  laserDriverEnabled = true;
}

void turnOnLaser() {
  if (laserEnabled) return;

  // Will return anyway if already active
  powerOnLaserDriver();

  digitalWrite(LASER_ENBL, HIGH);

  delay(1);

  digitalWrite(LASER_CURRENT_EN, HIGH);

  delay(1);

  digitalWrite(LASER_OUTSEL, HIGH);
  digitalWrite(LASER_OSCEN, HIGH);
  digitalWrite(LASER_WENRB, LOW);

  laserEnabled = true;
}

void turnOffLaser() {
  if (!laserEnabled) return;

  // Set control lines in preperation for disable
  digitalWrite(LASER_OUTSEL, LOW);
  digitalWrite(LASER_OSCEN, LOW);
  digitalWrite(LASER_WENRB, HIGH);
  digitalWrite(LASER_CURRENT_EN, LOW);

  delay(1);

  // After delay, disable the laser module completely
  digitalWrite(LASER_ENBL, LOW);

  delay(1);

  laserEnabled = false;
}

void turnOffLaserDriver() {
  if(!laserDriverEnabled) return;

  // Ensure laser is turned Off
  if (laserEnabled) turnOffLaser();

  // Turn off power to the device
  digitalWrite(LASER_EN, HIGH);

  // Set pins as inputs so they are floating/high-impedence
  pinMode(LASER_WENRB, INPUT);
  pinMode(LASER_ENBL, INPUT);
  pinMode(LASER_OSCEN, INPUT);
  pinMode(LASER_OUTSEL, INPUT);

  laserDriverEnabled = false;
}
