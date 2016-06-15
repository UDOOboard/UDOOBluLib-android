package org.udoo.udooblulib.sensor;

import java.util.UUID;

import static java.util.UUID.fromString;

/**
 * Created by Ekironji on 12/01/2016.
 */
public class UDOOBLE {

    public final static UUID
    // BASE UDOO BLE UUID: d7720000-79c6-452f-994c-9829da1a4229

            /* Button */
            UUID_BUT_SERV = fromString("d772e0ff-79c6-452f-994c-9829da1a4229"),
            UUID_KEY_SERV = fromString("d772ffe1-79c6-452f-994c-9829da1a4229"), // received from notify or indication
            UUID_KEY_DATA = fromString("d7722902-79c6-452f-994c-9829da1a4229"), // 00:00

    UUID_SENSORS_SERV                = fromString("d7728bf3-79c6-452f-994c-9829da1a4229"),
            UUID_SENSOR_CONF = fromString("d772c043-79c6-452f-994c-9829da1a4229"),
            UUID_SENSOR_DATA = fromString("d772dd91-79c6-452f-994c-9829da1a4229"),
            UUID_NOTIFICATION_PERI   = fromString("d77215f4-79c6-452f-994c-9829da1a4229"), // Period in tens of milliseconds


    /* IO Pins - Digital/Analog */
    UUID_IOPIN_SERV                 = fromString("d772a064-79c6-452f-994c-9829da1a4229"),
            UUID_IOPIN_PIN_MODE     = fromString("d772ace2-79c6-452f-994c-9829da1a4229"),
            UUID_IOPIN_DIGITAL_DATA = fromString("d7726bcf-79c6-452f-994c-9829da1a4229"), // 8 bit - 0: low, 1: high (0:A0...7:D7)
            UUID_IOPIN_ANALOG_READ  = fromString("d772233a-79c6-452f-994c-9829da1a4229"), //
            UUID_IOPIN_PWM_ANALOG_INDEX     = fromString("d7720955-79c6-452f-994c-9829da1a4229"),
    /* LED */
    UUID_LED_SERV = fromString("d772e605-79c6-452f-994c-9829da1a4229"),
            UUID_LED_RED = fromString("d772aed4-79c6-452f-994c-9829da1a4229"),
            UUID_LED_GREEN = fromString("d772cd51-79c6-452f-994c-9829da1a4229"),
            UUID_LED_YELLOW = fromString("d772db1a-79c6-452f-994c-9829da1a4229"),

    /* Temperature */
    UUID_TEM_SERV = fromString("d772c065-79c6-452f-994c-9829da1a4229"),
            UUID_TEM_DATA = fromString("d7720e25-79c6-452f-994c-9829da1a4229"),
            UUID_TEM_RESO = fromString("d77269bb-79c6-452f-994c-9829da1a4229"), // Resolution

    /* Accelerometer */
    UUID_ACC_SERV = fromString("d7728ceb-79c6-452f-994c-9829da1a4229"),
            UUID_ACC_DATA = fromString("d7729684-79c6-452f-994c-9829da1a4229"),

    /* Magnetometer */
    UUID_MAG_SERV = fromString("d772dd58-79c6-452f-994c-9829da1a4229"),
            UUID_MAG_DATA = fromString("d7721cb3-79c6-452f-994c-9829da1a4229"),

    /* Gyroscope */
    UUID_GYR_SERV = fromString("d7725a60-79c6-452f-994c-9829da1a4229"),
            UUID_GYR_DATA = fromString("d772ff58-79c6-452f-994c-9829da1a4229"),

    /* Barometer */
    UUID_BAR_SERV = fromString("d77287c3-79c6-452f-994c-9829da1a4229"),
            UUID_BAR_PRESS_DATA = fromString("d7727fdf-79c6-452f-994c-9829da1a4229"),
            UUID_BAR_ALTIT_DATA = fromString("d772d5aa-79c6-452f-994c-9829da1a4229"),

    UUID_HUM_SERV = fromString("d772bcbf-79c6-452f-994c-9829da1a4229"),
            UUID_HUM_DATA = fromString("d7720ce2-79c6-452f-994c-9829da1a4229"),
            UUID_TEMP_DATA = fromString("d7720e25-79c6-452f-994c-9829da1a4229"),
            UUID_HUM_RES = fromString("d77269bb-79c6-452f-994c-9829da1a4229"),

    UUID_AMB_LIG_SERV = fromString("d772aa45-79c6-452f-994c-9829da1a4229"),
            UUID_AMB_LIG_DATA = fromString("d77296a2-79c6-452f-994c-9829da1a4229");
}

