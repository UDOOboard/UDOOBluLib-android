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
    public final static int NOTIFICATIONS_PERIOD = 100;

    // Activity
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

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
