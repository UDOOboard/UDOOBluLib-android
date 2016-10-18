package org.udoo.udooblulib.manager;

import android.content.Context;

import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.interfaces.IBluManagerCallback;
import org.udoo.udooblulib.interfaces.INotificationListener;
import org.udoo.udooblulib.interfaces.IReaderListener;
import org.udoo.udooblulib.interfaces.OnBluOperationResult;
import org.udoo.udooblulib.interfaces.OnResult;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.scan.BluScanCallBack;

import java.util.Map;

/**
 * Created by harlem88 on 31/05/16.
 */

public interface UdooBluManager {
    enum SENSORS {ACC ,MAGN, GYRO,TEMP,BAR,HUM, AMB_LIG, RES}

    void init(Context context);
    void setIBluManagerCallback(IBluManagerCallback iBluManagerCallback);
    void scanLeDevice(boolean enable, BluScanCallBack scanCallback);
    void connect(String address, IBleDeviceListener iBleDeviceListener);
    void disconnect(String address);
    boolean bond(String address);
    boolean discoveryServices(String address);
    boolean [] getSensorDetected();
    boolean isSensorDetected(SENSORS sensors);

    void saveBluItem(Context context, String address, String name);
    void getBluItem(Context context, String address, OnResult<String> itemResult);
    void getBluItems(Context context, OnResult<Map<String, String>> onResult);

    boolean turnLed(String address, int color, byte func, int millis);

    void setIoPinMode(String address, final OnBluOperationResult<Boolean> onResultListener, IOPin... ioPins);
    void writeDigital(final String address, final OnBluOperationResult<Boolean> onBluOperationResult, final IOPin... ioPins);
    void writePwm(final String address, final IOPin.IOPIN_PIN pin, final int freq, final int dutyCycle, final OnBluOperationResult<Boolean> onResultListener);


    void readDigital(String address, IReaderListener<byte[]> readerListener, IOPin... pin);
    void readAnalog(final String address, final IOPin.IOPIN_PIN pin, final IReaderListener<byte[]> iReaderListener);
    void readAccelerometer(String address, IReaderListener<byte[]> readerListener);
    void readGyroscope(String address, IReaderListener<byte[]> readerListener);
    void readMagnetometer(String address, IReaderListener<byte[]> readerListener);
    void readBarometer(String address, IReaderListener<byte[]> readerListener);
    void readTemperature(String address, IReaderListener<byte[]> onCharacteristicsListener);
    void readHumidity(String address, IReaderListener<byte[]> onCharacteristicsListener);
    void readAmbientLight(String address, IReaderListener<byte[]> onCharacteristicsListener);

    void subscribeNotificationAccelerometer(String address, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationAccelerometer(String address, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationAccelerometer(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationGyroscope(String address, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationGyroscope(String address, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationGyroscope(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationMagnetometer(String address, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationMagnetometer(String address, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationMagnetometer(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationBarometer(String address, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationBarometer(String address, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationBarometer(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationTemperature(String address, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationTemperature(String address, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationTemperature(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationHumidity(String address, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationHumidity(String address, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationHumidity(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationAmbientLight(String address, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationAmbientLight(String address, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationAmbientLight(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationAnalog(String address, IOPin.IOPIN_PIN pin, INotificationListener<byte[]> notificationListener);
    void subscribeNotificationAnalog(String address, IOPin.IOPIN_PIN pin, INotificationListener<byte[]> notificationListener, int period);
    void unSubscribeNotificationAnalog(String address, OnBluOperationResult<Boolean> operationResult);

    void subscribeNotificationDigital(String address, INotificationListener<byte[]> notificationListener);
    void unSubscribeNotificationDigital(String address, OnBluOperationResult<Boolean> operationResult);

    /**
     * @param pin
     * @param freq value 3 to 24000000 (24 MHz)
     * @param dutyCycle value 0 to 100
     * */
    boolean pwmWrite(IOPin.IOPIN_PIN pin, int freq, int dutyCycle);

}
