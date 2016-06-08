package org.udoo.udooblulib.manager;

import android.content.Context;

import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.interfaces.INotificationListener;
import org.udoo.udooblulib.interfaces.OnCharacteristicsListener;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.scan.BluScanCallBack;
import org.udoo.udooblulib.sensor.UDOOBLESensor;

/**
 * Created by harlem88 on 31/05/16.
 */

public interface UdooBluManager {

    void init(Context context);
    void setIBluManagerCallback(UdooBluManagerImpl.IBluManagerCallback iBluManagerCallback);
    void scanLeDevice(boolean enable, BluScanCallBack scanCallback);
    void connect(String address, IBleDeviceListener iBleDeviceListener);
    void disconnect(String address);
    boolean bond(String address);
    boolean discoveryServices(String address);

    boolean enableNotification(String address, boolean enable, UDOOBLESensor sensor, OnCharacteristicsListener onCharacteristicsListener);
    boolean setNotificationPeriod(String address, UDOOBLESensor sensor);
    boolean setNotificationPeriod(String address, UDOOBLESensor sensor, int period);

    boolean turnLed(String address, int color, byte func, int millis);

    boolean setPinMode(IOPin.IOPIN_PIN pin, IOPin.IOPIN_MODE mode);
    boolean digitalWrite(IOPin.IOPIN_PIN pin, IOPin.IOPIN_DIGITAL_VALUE value);
    boolean digitalRead(IOPin.IOPIN_PIN pin);
    boolean analogRead(IOPin.IOPIN_PIN pin);


    void readAccelerometer(OnCharacteristicsListener onCharacteristicsListener);
    void subscribeNotificationAccelerometer(INotificationListener<Integer> notificationListener);
    void subscribeNotificationAccelerometer(INotificationListener<Integer> notificationListener, int period);

    void readGyroscope(OnCharacteristicsListener onCharacteristicsListener);
    void subscribeNotificationGyroscope(INotificationListener<Integer> notificationListener);
    void subscribeNotificationGyroscope(INotificationListener<Integer> notificationListener, int period);

    void readMagnetometer(OnCharacteristicsListener onCharacteristicsListener);
    void subscribeNotificationMagnetometer(INotificationListener<Integer> notificationListener);
    void subscribeNotificationMagnetometer(INotificationListener<Integer> notificationListener, int period);

    void readBarometer(OnCharacteristicsListener onCharacteristicsListener);
    void subscribeNotificationBarometer(INotificationListener<Integer> notificationListener);
    void subscribeNotificationBarometer(INotificationListener<Integer> notificationListener, int period);

    void readTemparature(OnCharacteristicsListener onCharacteristicsListener);
    void subscribeNotificationTemparature(INotificationListener<Integer> notificationListener);
    void subscribeNotificationTemparature(INotificationListener<Integer> notificationListener, int period);

    void readHumidity(OnCharacteristicsListener onCharacteristicsListener);
    void subscribeNotificationHumidity(INotificationListener<Integer> notificationListener);
    void subscribeNotificationHumidity(INotificationListener<Integer> notificationListener, int period);

    void readAmbientLight(OnCharacteristicsListener onCharacteristicsListener);
    void subscribeNotificationAmbientLight(INotificationListener<Integer> notificationListener);
    void subscribeNotificationAmbientLight(INotificationListener<Integer> notificationListener, int period);

    /**
     * @param pin
     * @param freq value 3 to 24000000 (24 MHz)
     * @param dutyCycle value 0 to 100
     * */
    boolean pwmWrite(IOPin.IOPIN_PIN pin, int freq, int dutyCycle);

}
