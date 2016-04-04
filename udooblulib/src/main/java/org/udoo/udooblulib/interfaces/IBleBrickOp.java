package org.udoo.udooblulib.interfaces;

import org.udoo.udooblulib.sensor.UDOOBLESensor;

import java.util.Observer;
import java.util.UUID;

/**
 * Created by harlem88 on 17/02/16.
 */
public interface IBleBrickOp {
    void readBleCharacteristic(UUID service, UUID characteristic, Observer observer);
    void writeBleCharacteristic(UUID service, UUID characteristic, byte[] msg, Observer observer) throws InterruptedException;
    boolean turnLed(int color, byte func, int millis);
    boolean readLed(int color);
    boolean readDigital();
    boolean writeConfigDigital();
    boolean writeDigital(boolean enable);
    boolean notification(UDOOBLESensor sensor, boolean enable);
    void notifications(boolean enable);
}
