package org.udoo.udooblulib.model;

import org.udoo.udooblulib.utils.BitUtility;

import java.io.Serializable;

/**
 * Created by harlem88 on 23/02/16.
 */
public class IOPin implements Serializable{
    public enum PIN {D7, D6, A5, A4, A3, A2, A1, A0}
    public enum MODE {DIGITAL_OUTPUT, DIGITAL_INPUT, ANALOG_INPUT, DIGITAL_PWM}
    public enum DIGITAL_VALUE {LOW, HIGH}
    public enum INDEX_VALUE {ANALOG, PWM}

    public boolean isAnalog;
    public boolean isOutput;
    public boolean isNotification;
    public PIN pin;
    public MODE mode;
    public DIGITAL_VALUE digitalValue;
    public INDEX_VALUE indexValue;
    public short analogValue;

    private IOPin(){

    }

    private IOPin(PIN iopin, MODE _mode, DIGITAL_VALUE value){
        this.pin = iopin;
        this.mode = _mode;
        this.digitalValue = value;
    }

    public static IOPin Builder(PIN iopin, MODE _mode) {
        IOPin ioPin = new IOPin(iopin, _mode, DIGITAL_VALUE.LOW);
        if (_mode.compareTo(MODE.ANALOG_INPUT) == 0) {
            ioPin.indexValue = INDEX_VALUE.ANALOG;
        } else if (_mode.compareTo(MODE.DIGITAL_PWM) == 0) {
            ioPin.indexValue = INDEX_VALUE.PWM;
        }
        return ioPin;
    }


    public static IOPin Builder(PIN iopin, DIGITAL_VALUE value){
        return new IOPin(iopin, MODE.DIGITAL_OUTPUT, value);
    }


    public static IOPin Builder(PIN iopin, INDEX_VALUE iopinIndexValue){
        IOPin ioPin = new IOPin();
        ioPin.pin = iopin;
        ioPin.indexValue = iopinIndexValue;
        return ioPin;
    }

//    public static IOPin Builder(String name, boolean isAnalog, byte config, int pos) {
//
//        IOPin ioPin = new IOPin();
//        ioPin.name = name;
//        int valuePin = (1 << pos) & config;
//        ioPin.value = "0";
//        /* digital position d7-d6 */
//        if (pos >= 6 || !isAnalog) {
//            ioPin.isAnalog = false;
//            ioPin.isOutput = valuePin == 0;
//            ioPin.isNotification = false;
//        } else {
//            ioPin.isAnalog = true;
//            ioPin.isOutput = false;
//            ioPin.isNotification = true;
//        }
//
//        return ioPin;
//    }

    public boolean isReadPin() {
        return isAnalog || !isOutput;
    }

    public boolean isWritePin() {
        return isOutput || !isAnalog;
    }


    public byte getPinValue(){
        return GetPinValue(pin);
    }

    public byte getIndexValue(){
        return GetIndexValue(pin, indexValue);
    }


    public byte getPinMode(){
        return GetModeValue(mode);
    }

    public short getShortDigitalValue() {
        return GetDigitalValue(digitalValue);
    }

    public static byte GetPinValue(PIN pin) {
        byte value = 0;

        switch (pin) {
            case D7:
                value = 7;
                break;
            case D6:
                value = 6;
                break;
            case A5:
                value = 5;
                break;
            case A4:
                value = 4;
                break;
            case A3:
                value = 3;
                break;
            case A2:
                value = 2;
                break;
            case A1:
                value = 1;
                break;
            case A0:
                value = 0;
                break;
        }
        return value;
    }

    public static byte GetIndexValue(PIN pin, INDEX_VALUE iopinIndexValue) {
        byte value = 0;

        switch (pin) {
            case D7:
                value =  iopinIndexValue == INDEX_VALUE.ANALOG ? BitUtility.ToUnsignedByte(0xff) : 7;
                break;
            case D6:
                value =  iopinIndexValue == INDEX_VALUE.ANALOG ? BitUtility.ToUnsignedByte(0xff) : 6;
                break;
            case A5:
                value = 5;
                break;
            case A4:
                value = 4;
                break;
            case A3:
                value = 3;
                break;
            case A2:
                value = 2;
                break;
            case A1:
                value = 1;
                break;
            case A0:
                value = 0;
                break;
        }
        return value;
    }

    public static byte GetModeValue(MODE mode) {
        byte value = 0;
        switch (mode) {
            case DIGITAL_OUTPUT:
                value = 0;
                break;
            case DIGITAL_INPUT:
                value = 1;
                break;
            case ANALOG_INPUT:
                value = 2;
                break;
            case DIGITAL_PWM:
                value = 3;
                break;
        }
        return value;
    }

    public static DIGITAL_VALUE GetDigitalValue(int dvalue) {
        DIGITAL_VALUE value = null;
        switch (dvalue) {
            case 0:
                value = DIGITAL_VALUE.LOW;
                break;
            case 1:
                value = DIGITAL_VALUE.HIGH;
                break;
        }
        return value;
    }

    public static PIN GetPin(int pin) {
        PIN value = null;
        switch (pin) {
            case 0:
                value = PIN.A0;
                break;
            case 1:
                value = PIN.A1;
                break;
            case 2:
                value = PIN.A2;
                break;
            case 3:
                value = PIN.A3;
                break;
            case 4:
                value = PIN.A4;
                break;
            case 5:
                value = PIN.A5;
                break;
            case 6:
                value = PIN.D6;
                break;
            case 7:
                value = PIN.D7;
                break;
        }
        return value;
    }

    public static short GetDigitalValue(DIGITAL_VALUE value){
        return (short) (value == DIGITAL_VALUE.HIGH ? 1 : 0);
    }
}
