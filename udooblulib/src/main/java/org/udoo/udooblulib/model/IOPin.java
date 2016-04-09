package org.udoo.udooblulib.model;

import org.udoo.udooblulib.sensor.Constant;

/**
 * Created by harlem88 on 23/02/16.
 */
public class IOPin {
    public boolean isAnalog;
    public String value;
    public boolean isOutput;
    public boolean isNotification;
    public String name;
    public Constant.IOPIN iopin;

    public static IOPin Builder(byte pos) {
        IOPin ioPin = new IOPin();
        if (pos == 0) ioPin.iopin = Constant.IOPIN.A0;
        else if (pos == 2) ioPin.iopin = Constant.IOPIN.A1;
        else if (pos == 4) ioPin.iopin = Constant.IOPIN.A2;
        else if (pos == 8) ioPin.iopin = Constant.IOPIN.A3;
        else if (pos == 16) ioPin.iopin = Constant.IOPIN.A4;
        else if (pos == 32) ioPin.iopin = Constant.IOPIN.A5;
        else if (pos == 64) ioPin.iopin = Constant.IOPIN.D6;
        return ioPin;
    }


    public static IOPin Builder(String name, boolean isAnalog, byte config, int pos) {
        IOPin ioPin = new IOPin();
        ioPin.name = name;
        int valuePin = (1 << pos) & config;
        ioPin.value = "0";
        /* digital position d7-d6 */
        if (pos >= 6 || !isAnalog) {
            ioPin.isAnalog = false;
            ioPin.isOutput = valuePin == 0;
            ioPin.isNotification = false;
        } else {
            ioPin.isAnalog = true;
            ioPin.isOutput = false;
            ioPin.isNotification = true;
        }

        return ioPin;
    }

    public boolean isReadPin() {
        return isAnalog || !isOutput;
    }

    public boolean isWritePin() {
        return isOutput || !isAnalog;
    }


}
