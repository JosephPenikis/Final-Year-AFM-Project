#ifndef DAC_CONTROL
#define DAC_CONTROL

#include <arduino.h>
#include <SPI.h>

/////////REGISTER COMMANDS AND ADDRESSES/////
#define COMMAND_WRITE_INPUT     0x01
#define COMMAND_DAC_UPDATE      0x02
#define COMMAND_DAC_WR_UPDATE   0x03
#define COMMAND_DAC_POWER       0x04
#define COMMAND_LDAC_MASK       0x05
#define COMMAND_SOFT_RESET      0x06
#define COMMAND_GAIN_SET_REG    0x07
#define COMMAND_DCEN            0x08
#define COMMAND_READBACK        0x09
#define COMMAND_UPDATE_ALL_DAC        0x10
#define COMMAND_UPDATE_ALL_DAC_INPUT  0x11

#define IN_REG_0  0x00
#define IN_REG_1  0x01
#define IN_REG_2  0x02
#define IN_REG_3  0x03
#define IN_REG_4  0x04
#define IN_REG_5  0x05
#define IN_REG_6  0x06
#define IN_REG_7  0x07

/*DAC CLASS*/
class DAC {
  public:
    const int var_spi_order = MSBFIRST;
    const int var_spi_mode = SPI_MODE2;
    const long var_speed_max = (2500000);

    int slaveSelectPin;
    int n_LDAC;

    void init(int ldac_pin, int ssPin) {

      slaveSelectPin = ssPin;
      n_LDAC = ldac_pin;

      // initalize the  data ready and chip select pins:
      pinMode(slaveSelectPin, OUTPUT);
      digitalWrite(slaveSelectPin, HIGH);
      pinMode(n_LDAC, OUTPUT);
      digitalWrite(n_LDAC, HIGH);
    }

    void setValueX(int32_t value) {
      int32_t ref = 0x7FFF;   // reference for zero point
      uint16_t data_1 = 0,
               data_3 = 0;

      data_1 = ref - value;
      data_3 = ref + value;
      SPI.beginTransaction(SPISettings(var_speed_max, var_spi_order, var_spi_mode));
      spiWrite(COMMAND_DAC_WR_UPDATE, IN_REG_1, data_1);
      spiWrite(COMMAND_DAC_WR_UPDATE, IN_REG_3, data_3);

      digitalWrite(n_LDAC, LOW);
      digitalWrite(n_LDAC, HIGH);
      SPI.endTransaction();
    }

    void setValueY(int32_t value) {
      int32_t ref = 0x7FFF;   // reference for zero point
      uint16_t data_0 = 0,
               data_2 = 0;

      data_0 = ref - value;
      data_2 = ref + value;

      SPI.beginTransaction(SPISettings(var_speed_max, var_spi_order, var_spi_mode));
      spiWrite(COMMAND_UPDATE_ALL_DAC_INPUT, IN_REG_0, data_0);
      spiWrite(COMMAND_UPDATE_ALL_DAC_INPUT, IN_REG_2, data_2);
      digitalWrite(n_LDAC, LOW);
      digitalWrite(n_LDAC, HIGH);
      SPI.endTransaction();
    }

    void setValueZ(int32_t value) {
      int32_t ref = 0x7FFF;
      uint16_t data = 0;

      data = ref + value;

      SPI.beginTransaction(SPISettings(var_speed_max, var_spi_order, var_spi_mode));
      spiWrite(COMMAND_UPDATE_ALL_DAC_INPUT, IN_REG_0, data);
      digitalWrite(n_LDAC, LOW);
      digitalWrite(n_LDAC, HIGH);
      SPI.endTransaction();
    }

    void spiWrite(uint8_t command, uint8_t address, uint16_t reg_data) {
      // take the SS pin low to select the chip:
      digitalWrite(slaveSelectPin, LOW);
      //  send in the address and value via SPI:
      SPI.transfer((command << 4) | address);
      SPI.transfer16(reg_data);
      // take the SS pin high to de-select the chip:
      digitalWrite(slaveSelectPin, HIGH);
    }

    void spiRead(uint8_t address) {
      // take the SS pin low to select the chip:
      digitalWrite(slaveSelectPin, LOW);
      //  send in the address and value via SPI:
      SPI.transfer((COMMAND_READBACK << 4) | address);
      SPI.transfer16(0x0000);
      digitalWrite(slaveSelectPin, HIGH);
      digitalWrite(slaveSelectPin, LOW);
      SPI.transfer(0x00);
      SPI.transfer16(0x0000);
      // take the SS pin high to de-select the chip:
      digitalWrite(slaveSelectPin, HIGH);
    }
};


#endif











