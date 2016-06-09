package org.udoo.udooblulib.manager;

import android.content.Context;

import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.interfaces.INotificationListener;
import org.udoo.udooblulib.interfaces.IReaderListener;
import org.udoo.udooblulib.interfaces.OnBluOperationResult;
import org.udoo.udooblulib.interfaces.OnCharacteristicsListener;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.scan.BluScanCallBack;
import org.udoo.udooblulib.sensor.UDOOBLESensor;
import org.udoo.udooblulib.utils.Point3D;

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

    void setIoPinMode(String address, final OnBluOperationResult<Boolean> onResultListener, IOPin... ioPins);
    boolean digitalWrite(IOPin.IOPIN_PIN pin, IOPin.IOPIN_DIGITAL_VALUE value);
    boolean digitalRead(IOPin.IOPIN_PIN pin);
    boolean analogRead(IOPin.IOPIN_PIN pin);


    void readAccelerometer(String address, IReaderListener<Point3D> readerListener);
    void subscribeNotificationAccelerometer(String address, INotificationListener<Point3D> notificationListener);
    void subscribeNotificationAccelerometer(String address, INotificationListener<Point3D> notificationListener, int period);

    void readGyroscope(IReaderListener<Point3D> readerListener);
    void subscribeNotificationGyroscope(INotificationListener<Point3D> notificationListener);
    void subscribeNotificationGyroscope(INotificationListener<Point3D> notificationListener, int period);

    void readMagnetometer(IReaderListener<Point3D> readerListener);
    void subscribeNotificationMagnetometer(INotificationListener<Integer> notificationListener);
    void subscribeNotificationMagnetometer(INotificationListener<Integer> notificationListener, int period);

    void readBarometer(IReaderListener<Integer> readerListener);
    void subscribeNotificationBarometer(INotificationListener<Integer> notificationListener);
    void subscribeNotificationBarometer(INotificationListener<Integer> notificationListener, int period);

    void readTemparature(IReaderListener<Float> onCharacteristicsListener);
    void subscribeNotificationTemparature(INotificationListener<Float> notificationListener);
    void subscribeNotificationTemparature(INotificationListener<Float> notificationListener, int period);

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
