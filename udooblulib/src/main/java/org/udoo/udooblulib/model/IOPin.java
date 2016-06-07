package org.udoo.udooblulib.model;

import com.google.common.primitives.UnsignedBytes;

import org.udoo.udooblulib.sensor.Constant;
import org.udoo.udooblulib.utils.BitUtility;

/**
 * Created by harlem88 on 23/02/16.
 */
public class IOPin {
    public enum IOPIN_PIN {D7, D6, A5, A4, A3, A2, A1, A0}
    public enum IOPIN_MODE {DIGITAL_OUTPUT, DIGITAL_INPUT, ANALOG, PWM}
    public enum IOPIN_DIGITAL_VALUE{LOW, HIGH}
    public enum IOPIN_INDEX_VALUE{ANALOG, PWM}

    public boolean isAnalog;
    public boolean isOutput;
    public boolean isNotification;
    public IOPIN_PIN pin;
    public IOPIN_MODE mode;
    public IOPIN_DIGITAL_VALUE digitalValue;
    public IOPIN_INDEX_VALUE indexValue;

    private IOPin(){

    }

    private IOPin(IOPIN_PIN iopin, IOPIN_MODE iopin_mode, IOPIN_DIGITAL_VALUE value){
        this.pin = iopin;
        this.mode = iopin_mode;
        this.digitalValue = value;
    }

    public static IOPin Builder(IOPIN_PIN iopin, IOPIN_MODE iopin_mode){
        return new IOPin(iopin, iopin_mode, IOPIN_DIGITAL_VALUE.LOW);
    }


    public static IOPin Builder(IOPIN_PIN iopin, IOPIN_DIGITAL_VALUE value){
        return new IOPin(iopin, IOPIN_MODE.DIGITAL_OUTPUT, value);
    }


    public static IOPin Builder(IOPIN_PIN iopin, IOPIN_INDEX_VALUE iopinIndexValue){
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

    public short getDigitalValue() {
        return GetDigitalValue(digitalValue);
    }

    public static byte GetPinValue(IOPin.IOPIN_PIN pin) {
        byte value = 0;

        switch (pin) {
            case D7:
                value = BitUtility.ToUnsignedByte(128);
                break;
            case D6:
                value = 64;
                break;
            case A5:
                value = 32;
                break;
            case A4:
                value = 16;
                break;
            case A3:
                value = 8;
                break;
            case A2:
                value = 4;
                break;
            case A1:
                value = 2;
                break;
            case A0:
                value = 1;
                break;
        }
        return value;
    }

    public static byte GetIndexValue(IOPin.IOPIN_PIN pin, IOPIN_INDEX_VALUE iopinIndexValue) {
        byte value = 0;

        switch (pin) {
            case D7:
                value =  iopinIndexValue == IOPIN_INDEX_VALUE.ANALOG ? BitUtility.ToUnsignedByte(0xff) : 7;
                break;
            case D6:
                value =  iopinIndexValue == IOPIN_INDEX_VALUE.ANALOG ? BitUtility.ToUnsignedByte(0xff) : 6;
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

    public static byte GetModeValue(IOPIN_MODE mode) {
        byte value = 0;
        switch (mode) {
            case DIGITAL_OUTPUT:
                value = 0;
                break;
            case DIGITAL_INPUT:
                value = 1;
                break;
            case ANALOG:
                value = 2;
                break;
            case PWM:
                value = 3;
                break;
        }
        return value;
    }

    public static short GetDigitalValue(IOPIN_DIGITAL_VALUE value){
        return (short) (value == IOPIN_DIGITAL_VALUE.HIGH ? 1 : 0);
    }
}
