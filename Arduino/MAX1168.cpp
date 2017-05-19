#include "MAX1168.h"

// Slightly tweaked implementation of standard transfer16 function to improve
// performance during 16-bit transfers where endianness is different
static void transfer16fast(twoByte *buf, size_t count) {
  // Interestingly, the "nop"s do have a performance improvemnt of ~3us
  while (count-- > 0) {
      SPDR = 0x00;
      asm volatile("nop");
      while (!(SPSR & _BV(SPIF)));
      ((twoByte*)buf)->msb = SPDR;

      SPDR = 0x00;
      asm volatile("nop");
      while (!(SPSR & _BV(SPIF)));
      ((twoByte*)buf++)->lsb = SPDR;
  }
}

MAX1168::MAX1168(uint8_t ssPin, uint8_t EOCpin, powerMode power) {
  // Grab port adress and bit flag for pin and save them. Allows substantially
  // faster read/write ops
  slaveSelectPin.port = portOutputRegister(digitalPinToPort(ssPin));
  slaveSelectPin.bit = digitalPinToBitMask(ssPin);

  EndOfConversionPin.port = portInputRegister(digitalPinToPort(EOCpin));
  EndOfConversionPin.bit = digitalPinToBitMask(EOCpin);

  // Use pre existing arduino functions for this as we only do it once
  pinMode(ssPin, OUTPUT);

  spiSettings = SPISettings(MAX1168_MAX_SPI_CLK, MAX1168_BITORDER, MAX1168_SPI_MODE);

  // Driver currently implemented to ONLY utilise internal clock mode
  settingFlags = power | MAX1168_INTERNAL_CLOCK;
}

void MAX1168::begin() {
  SPI.begin();
}

void MAX1168::beginTransaction() {
  SPI.beginTransaction(spiSettings);
  fastSlaveSelectWrite(LOW);
}

void MAX1168::endTransaction() {
  fastSlaveSelectWrite(HIGH);
  SPI.endTransaction();
}

uint8_t MAX1168::fastEOCread() {
  // Fast read of pin
  return ((*EndOfConversionPin.port & EndOfConversionPin.bit) ? HIGH : LOW);
}

void MAX1168::fastSlaveSelectWrite(uint8_t s) {
  // Fast port write
  if (s == LOW) {
    *slaveSelectPin.port &= ~slaveSelectPin.bit;
  } else {
    *slaveSelectPin.port |= slaveSelectPin.bit;
  }
}

void MAX1168::readSingleChannel_16bit(void *a, uint8_t channel) {
  // In 16-bit mode, so create control byte using settings flags and channel number,
  // shift a byte to the left and send. Then transfer result back!
  SPI.transfer16((MAX1168_CHAN(channel) | settingFlags) << 8);
  // Wait for EOC flag to be set
  while (fastEOCread() == HIGH);
  // Read in the data!
  *((uint16_t*)a) = SPI.transfer16(0x0000);
}

void MAX1168::readMultipleChannels_16bit(void *a, channelStart start, uint8_t n) {
  // Similar to Single Channel func, except 'start' is pre-declared with the correct
  // control bits

  /*
   NOTE: The (x ? y : z) is a ternary operator essentially equavalant to
      if (x)
        y;
      else
        z;
  */
  SPI.transfer16((MAX1168_CHAN(start == CHANNEL_START_0 ? (n - 1) : (n + 3)) | start | settingFlags) << 8);
  // Wait for EOC flag to be set
  while (fastEOCread() == HIGH);
  // Now read in the data
  transfer16fast((twoByte*)a, n);
}
