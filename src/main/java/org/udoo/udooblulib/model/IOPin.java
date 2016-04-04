package org.udoo.udooblulib.model;

/**
 * Created by harlem88 on 23/02/16.
 */
public class IOPin {
    public boolean isAnalog;
    public String value;
    public boolean isOutput;
    public boolean isNotification;
    public String name;

    public static IOPin Builder(String name, boolean isAnalog, byte config, int pos){
        IOPin ioPin = new IOPin();
        ioPin.name = name;
        int valuePin = (1 << pos) & config;
        ioPin.value = "0";
        /* digital position d7-d6 */
        if(pos >= 6 || !isAnalog){
            ioPin.isAnalog = false;
            ioPin.isOutput = valuePin == 0;
            ioPin.isNotification = false;
        }else {
            ioPin.isAnalog = true;
            ioPin.isOutput = false;
            ioPin.isNotification = true;
        }

        return  ioPin;
    }

    public boolean isReadPin() {
        return isAnalog || !isOutput;
    }

    public boolean isWritePin(){
        return isOutput || !isAnalog;
    }


}
