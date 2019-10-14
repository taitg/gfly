import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.Lcd;

public class LCD {

  private final static int LCD_ROWS = 2;
  private final static int LCD_COLUMNS = 16;
  private final static int LCD_BITS = 4;

  private int handle;

  public LCD() {
    // initialize LCD
    handle = Lcd.lcdInit(LCD_ROWS, // number of row supported by LCD
        LCD_COLUMNS, // number of columns supported by LCD
        LCD_BITS, // number of bits used to communicate to LCD
        4, // LCD RS pin
        5, // LCD strobe pin
        6, // LCD data bit 1
        10, // LCD data bit 2
        11, // LCD data bit 3
        31, // LCD data bit 4
        0, // LCD data bit 5 (set to 0 if using 4 bit communication)
        0, // LCD data bit 6 (set to 0 if using 4 bit communication)
        0, // LCD data bit 7 (set to 0 if using 4 bit communication)
        0); // LCD data bit 8 (set to 0 if using 4 bit communication)

    // verify initialization
    if (handle == -1) {
      System.out.println(" ==>> LCD INIT FAILED");
      return;
    }

    // clear LCD
    Lcd.lcdClear(handle);
    Util.delay(1000);

    // write line 1 to LCD
    writeLine(0, "      GFLY      ");

    // write line 2 to LCD
    writeLine(1, "----------------");
  }

  public void writeLine(int lineNum, String line) {
    Lcd.lcdHome(handle);
    Lcd.lcdPosition(handle, 0, lineNum);
    Lcd.lcdPuts(handle, line);
  }
}