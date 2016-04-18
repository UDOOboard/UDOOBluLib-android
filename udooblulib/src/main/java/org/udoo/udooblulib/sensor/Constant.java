package org.udoo.udooblulib.sensor;

/**
 * Created by harlem88 on 15/02/16.
 */
public class Constant {


    final public static byte LED_ON = 0x01;
    final public static byte LED_OFF = 0x00;
    final public static byte BLINK_ON = 0x02;
    final byte BLINK_OFF = 0x00;
    final public static String BASE_UUID = "d7728bf3-79c6-452f-994c-9829da1a4229";
    final public static int GREEN_LED = 1;
    final public static int YELLOW_LED = 2;
    final public static int RED_LED = 3;
    public static final int GATT_TIMEOUT = 300; // milliseconds

    // Sensors parameter
    public final static int NOTIFICATIONS_PERIOD = 10;

    // Activity
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

    /* IOPIN */
    public enum IOPIN {D7, D6, A5, A4, A3, A2, A1, A0}
    public enum IOPIN_TYPE {DIGITAL, ANALOG}
    public enum IOPIN_MODE {INPUT, OUTPUT}
    public enum IOPIN_VALUE{LOW, HIGH}

    /**
     * Risolution value of temperature
     * 0 bit significant
     * 1 risolution
     */
    public static final float[][] TemperatureTable = {
            {9, 0.5f},
            {10, 0.25f},
            {11, 0.125f},
            {12, 0.0625f},
    };
}
