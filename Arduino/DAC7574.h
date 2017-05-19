#include "Arduino.h"
#include "Wire.h"

/*=========================================================================
    Default device address
    -----------------------------------------------------------------------*/
#define DAC7574_7BIT_DEVICE_ADDRESS   0x4C
/*=========================================================================*/

/*=========================================================================
    Control byte
    -----------------------------------------------------------------------*/
#define DAC7574_STORE_I2C_DATA        0x00
#define DAC7574_UPDATE_NOW            0x10
#define DAC7574_4CHAN_SYNC_UPDATE     0x20
#define DAC7574_BROADCAST_UPDATE      0x30

typedef enum {
  CHANNEL_A = 0x00,
  CHANNEL_B = 0x02,
  CHANNEL_C = 0x04,
  CHANNEL_D = 0x06
} DAC7574_Channel;

#define DAC7574_NORMAL_OPERATION      0x00
#define DAC7574_POWER_DOWN_FLAG       0x01
/*=========================================================================*/

// Fool-proofing enums to ensure properly passed values
typedef enum {
  I2C_STANDARD,
  I2C_FAST
} i2cMode;

typedef enum: uint8_t {
  A1_LOW_A0_LOW = 0x00,
  A1_LOW_A0_HIGH = 0x01,
  A1_HIGH_A0_LOW = 0x02,
  A1_HIGH_A0_HIGH = 0x03
} i2cAddress;

class DAC7574
{
protected:
  i2cMode mode;
  uint8_t address = DAC7574_7BIT_DEVICE_ADDRESS;
  uint8_t control;

public:
  DAC7574(i2cAddress addr, i2cMode m);

  void begin(uint8_t controlByte);

  void writeToChannel(uint16_t v, DAC7574_Channel channel);
};
