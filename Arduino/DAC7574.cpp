#include "DAC7574.h"

DAC7574::DAC7574(i2cAddress addr, i2cMode m) {
  address |= addr;
  mode = m;
}

void DAC7574::begin(uint8_t controlByte) {
  control = controlByte;

  Wire.begin();
  // Wire.setClock(1000000);
}

void DAC7574::writeToChannel(uint16_t v, DAC7574_Channel channel) {
  Wire.beginTransmission(address);
  Wire.write(control | channel);
  Wire.write((byte)((v >> 4) & 0xFF));
  Wire.write((byte)((v << 4) & 0xFF));
  Wire.endTransmission();
}
