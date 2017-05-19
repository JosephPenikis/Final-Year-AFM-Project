#include <SPI.h>
#include <Arduino.h>

/*=========================================================================
    CONTROL BYTE MASKS
    -----------------------------------------------------------------------*/
    #define MAX1168_CH_SEL_MASK     (0xE0)
    #define MAX1168_SCAN_MODE_MASK  (0x18)
    #define MAX1168_PD_MODE_MASK    (0x06)
    #define MAX1168_CLK_MODE_MASK   (0x01)
/*=========================================================================*/

/*=========================================================================
    CONTROL BYTE SETTINGS
    -----------------------------------------------------------------------*/
    // Macro definition to generate below
    #define MAX1168_CHAN(c) (((c) << 5) & MAX1168_CH_SEL_MASK)

    #define MAX1168_CHAN_AIN_0      MAX1168_CHAN(0)
    #define MAX1168_CHAN_AIN_1      MAX1168_CHAN(1)
    #define MAX1168_CHAN_AIN_2      MAX1168_CHAN(2)
    #define MAX1168_CHAN_AIN_3      MAX1168_CHAN(3)
    #define MAX1168_CHAN_AIN_4      MAX1168_CHAN(4)
    #define MAX1168_CHAN_AIN_5      MAX1168_CHAN(5)
    #define MAX1168_CHAN_AIN_6      MAX1168_CHAN(6)
    #define MAX1168_CHAN_AIN_7      MAX1168_CHAN(7)

    // Scan modes when using internal clock (Not for DSP mode or external)
    #define MAX1168_SCAN_MODE_SINGLE    (0x00)    // Single channel, no scan
    #define MAX1168_SCAN_MODE_0_TO_N    (0x08)    // Sequentially scan channels, 0 through N (N <= 7)
    #define MAX1168_SCAN_MODE_4_TO_N    (0x10)    // Sequentially scan channels, 4 through N (4 <= N <= 7)
    #define MAX1168_SCAN_MODE_INT_N_x8  (0x18)    // Scan Channel N eight times

    // Power down modes
    /*                  |    Between Conversions    |
    |   |   | Reference | Internal Ref | Ref Buffer | Typ. Suppply Current | Typ. wake-up
    =====================================================================================
      0   0   Internal        On            On                1mA                NA
      0   1   Internal        Off           Off               0.6uA             5ms
      1   0   Internal        On            Off               0.43mA            5ms
      1   1   external        Off <ALWAYS>  Off               0.6uA             NA
    */
    #define MAX1168_PD_RINT_ON_BUF_ON           (0x00)
    #define MAX1168_PD_RINT_OFF_BUF_OFF         (0x02)
    #define MAX1168_PD_RINT_ON_BUF_OFF          (0x04)
    #define MAX1168_PD_RINT_OFF_BUF_OFF_ALWAYS  (0x06)

    // Clock modes
    #define MAX1168_EXTERNAL_CLOCK      (0x00)
    #define MAX1168_INTERNAL_CLOCK      (0x01)
/*=========================================================================*/

// Max supported clock rate
#define MAX1168_MAX_SPI_CLK   (20000000)
#define MAX1168_SPI_MODE      SPI_MODE0
#define MAX1168_BITORDER      MSBFIRST

typedef enum
{
  RINT_ON_BUF_ON = MAX1168_PD_RINT_ON_BUF_ON,
  RINT_OFF_BUF_OFF = MAX1168_PD_RINT_OFF_BUF_OFF,
  RINT_ON_BUF_OFF = MAX1168_PD_RINT_ON_BUF_OFF,
  RINT_OFF_BUF_OFF_ALWAYS = MAX1168_PD_RINT_OFF_BUF_OFF_ALWAYS
} powerMode;

typedef enum
{
  CHANNEL_START_0 = MAX1168_SCAN_MODE_0_TO_N,
  CHANNEL_START_4 = MAX1168_SCAN_MODE_4_TO_N
} channelStart;

// Union structure used to aid byte reversal from big-endian to little-endian
union twoByte {
  uint16_t val;
  struct { uint8_t msb; uint8_t lsb; };
};

struct pinData {
  uint8_t bit;
  volatile uint8_t* port;
};

class MAX1168
{
// Members that can only be accessed by this class and any children of it
// should the class be extended at a later date
protected:
  pinData slaveSelectPin;
  pinData EndOfConversionPin;
  SPISettings spiSettings;
  uint8_t settingFlags;

  // Faster implementation of port read/write than arduino's standard approach
  uint8_t fastEOCread(void);
  void fastSlaveSelectWrite(uint8_t);

  /*
      Functions for 8 & 16 bit transfer modes, the first argument is a pointer to
      the memory location to write into. It is assumed that enough memory is allocated
      before calling the function as it cannot check!

      'void*' is type agnostic so accepts any pointer meaning care must be taken when
      passing values, i.e. passing a uint8_t pointer to the 16_bit function is a bad
      idea (unless it is an array of size greater than 2)

      Not the simplest approach, but allows performance to be improved by remapping
      the functions rather than branching within a function depending on the selected
      mode.
  */

  // 8-bit mode function
  //void readSingleChannel_8bit(void* a, uint8_t channel);
  //void readMultipleChannels_8bit(void* a, channelStart start, uint8_t n);

// All publicly accessable members here
public:
  MAX1168(uint8_t ssPin, uint8_t EOCpin, powerMode power);
  void begin(void);
  void beginTransaction(void);
  void endTransaction(void);
  // Function pointers which are accessible to the outside
  // These are assigned on initialisation depending on whether 16/32 bit mode
  // Could have been done with 'ifs', but this is removes the performance impact
  // of said approach

  // 16-bit operations
  void readSingleChannel_16bit(void* a, uint8_t channel);
  void readMultipleChannels_16bit(void* a, channelStart start, uint8_t n);

// Add any private members here, only visible to members WITHIN the class itself
private:

};
