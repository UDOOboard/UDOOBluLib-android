package org.udoo.udooblulib;

import org.udoo.udooblulib.exceptions.UdooBluException;
import org.udoo.udooblulib.interfaces.INotificationListener;
import org.udoo.udooblulib.interfaces.IReaderListener;
import org.udoo.udooblulib.interfaces.OnBluOperationResult;
import org.udoo.udooblulib.manager.UdooBluManager;
import org.udoo.udooblulib.manager.UdooBluManager.SENSORS;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.sensor.Constant;
import org.udoo.udooblulib.sensor.UDOOBLESensor;

/**
 * Created by harlem88 on 28/03/17.
 */

public class UdooBlu {
    private String mAddress;
    private UdooBluManager mUdooBluManager;
    private IOPin[] mIOPins;
    /***
     * 0 Accelerometer
     * 1 Magnetometer
     * 2 Gyroscope
     * 3 Temperature
     * 4 Barometer
     * 5 Humidity
     * 6 Light
     * 7 Reserved
     */
    private boolean[] sensorsDetected;
    private boolean[] sensorsEnabled = new boolean[8];


    /**
     * 0x00 Digital Output
     * 0x01 Digital Input
     * 0x02 Analog
     * 0x03 PWM
     **/
    private byte iOPinConfig[] = new byte[8];
    private byte indexAnalogConfig;


    public UdooBlu(String address, boolean[] sensorsDetected, UdooBluManager udooBluManager){
        mAddress = address;
        mUdooBluManager = udooBluManager;
        mIOPins = new IOPin[8];
        this.sensorsDetected = sensorsDetected;
    }

    public void readFirmwareVersion(IReaderListener<byte[]> readerListener) {
        if(mUdooBluManager!= null) mUdooBluManager.readFirmwareVersion(mAddress, readerListener);
    }

    public boolean[] getSensorDetected() {
        return sensorsDetected;
    }

    public boolean isSensorDetected(SENSORS sensor) {
        return sensorsDetected[sensor.ordinal()];
    }

    public void blinkLed(int color, boolean blink) {
        if(mUdooBluManager!= null) mUdooBluManager.blinkLed(mAddress, color, blink);
    }

    public void setIoPinMode(final OnBluOperationResult<Boolean> onResultListener, IOPin... ioPins) {
        final IOPin[] tmpIOPins = mergeWithLocalIOPinConfig(ioPins);
        if(mUdooBluManager!= null) mUdooBluManager.setIoPinMode(mAddress, new OnBluOperationResult<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                mIOPins = tmpIOPins.clone();
                if(onResultListener != null) onResultListener.onSuccess(aBoolean);
            }

            @Override
            public void onError(UdooBluException runtimeException) {
                if(onResultListener != null) onResultListener.onError(runtimeException);
            }
        }, tmpIOPins);
    }

    public void digitalRead(IReaderListener<byte[]> iReaderListener, int pin , int value) {
        IOPin.IOPIN_PIN ioPin = IOPin.GetPin(pin);
        IOPin.IOPIN_DIGITAL_VALUE ioValue = IOPin.GetDigitalValue(value);

        if(ioPin != null && ioValue != null)
            digitalRead(iReaderListener, IOPin.Builder(ioPin, ioValue));
    }

    public void digitalWrite(int pin , int value) {
        IOPin.IOPIN_PIN ioPin = IOPin.GetPin(pin);
        IOPin.IOPIN_DIGITAL_VALUE ioValue = IOPin.GetDigitalValue(value);

        if(ioPin != null && ioValue != null)
            digitalWrite(null, IOPin.Builder(ioPin, ioValue));
    }

    public void digitalWrite(final OnBluOperationResult<Boolean> onBluOperationResult, final IOPin... ioPins) {
        if (iOPinVerifier(IOPin.IOPIN_MODE.DIGITAL_OUTPUT, ioPins)) {
            if(mUdooBluManager!= null) mUdooBluManager.writeDigital(mAddress, onBluOperationResult, ioPins);
        }else{
            iOPinModeBuilder(IOPin.IOPIN_MODE.DIGITAL_OUTPUT, ioPins);
            setIoPinMode(new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    setLocaliOPinConfig(IOPin.IOPIN_MODE.DIGITAL_OUTPUT, ioPins);
                    if(mUdooBluManager!= null) mUdooBluManager.writeDigital(mAddress, onBluOperationResult, ioPins);
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    if (runtimeException != null)
                        onBluOperationResult.onError(runtimeException);
                }
            }, ioPins);
        }
    }


    public void pwmWrite(int pin , final int freq, final int dutyCycle, final OnBluOperationResult<Boolean> onResultListener) {
        IOPin.IOPIN_PIN ioPin = IOPin.GetPin(pin);
        if(ioPin != null) pwmWrite(ioPin, freq, dutyCycle, onResultListener);
    }


    public void pwmWrite(IOPin.IOPIN_PIN pin, final int freq, final int dutyCycle, final OnBluOperationResult<Boolean> onResultListener) {
        final IOPin ioPin = IOPin.Builder(pin, IOPin.IOPIN_MODE.PWM);
        if (iOPinVerifier(IOPin.IOPIN_MODE.PWM, ioPin)) {
            if (indexAnalogConfig == ioPin.getIndexValue()) {
                if(mUdooBluManager != null) mUdooBluManager.writePwm(mAddress, freq, dutyCycle, onResultListener);
            } else {
                if (mUdooBluManager != null)
                    mUdooBluManager.setPinAnalogOrPwmIndex(mAddress, IOPin.Builder(pin, IOPin.IOPIN_INDEX_VALUE.PWM), new OnBluOperationResult<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            if (aBoolean) {
                                indexAnalogConfig = ioPin.getIndexValue();
                                if (mUdooBluManager != null)
                                    mUdooBluManager.writePwm(mAddress, freq, dutyCycle, onResultListener);
                            } else if (onResultListener != null) {
                                onResultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                            }
                        }

                        @Override
                        public void onError(UdooBluException runtimeException) {
                            if (runtimeException != null && onResultListener != null)
                                onResultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                        }
                    });
            }
        }else{
            iOPinModeBuilder(IOPin.IOPIN_MODE.PWM, ioPin);
            setIoPinMode(new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    if (aBoolean) {
                        setLocaliOPinConfig(IOPin.IOPIN_MODE.PWM, ioPin);
                        if(mUdooBluManager != null) mUdooBluManager.writePwm(mAddress, freq, dutyCycle, onResultListener);
                    } else if (onResultListener != null) {
                        onResultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                    }
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    if (runtimeException != null && onResultListener != null)
                        onResultListener.onError(runtimeException);
                }
            }, ioPin);
        }
    }

    public void digitalRead(final IReaderListener<byte[]> readerListener, final IOPin... pins) {
        if (iOPinVerifier(IOPin.IOPIN_MODE.DIGITAL_INPUT, pins)) {
            if (mUdooBluManager != null)
                mUdooBluManager.readDigital(mAddress, readerListener);
        } else {
            iOPinModeBuilder(IOPin.IOPIN_MODE.DIGITAL_INPUT, pins);
            setIoPinMode(new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    setLocaliOPinConfig(IOPin.IOPIN_MODE.DIGITAL_INPUT, pins);
                    if (mUdooBluManager != null)
                        mUdooBluManager.readDigital(mAddress, readerListener);
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    if (runtimeException != null && readerListener != null)
                        readerListener.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));
                }
            }, pins);
        }
    }

    public void analogRead(int pin , final IReaderListener<byte[]> iReaderListener) {
        IOPin.IOPIN_PIN ioPin = IOPin.GetPin(pin);
        if(ioPin != null) analogRead(ioPin, iReaderListener);
    }

    public void analogRead(IOPin.IOPIN_PIN pin, final IReaderListener<byte[]> iReaderListener) {
        final IOPin ioPin = IOPin.Builder(pin, IOPin.IOPIN_MODE.ANALOG);
        if (iOPinVerifier(ioPin)) {
            if (indexAnalogConfig == ioPin.getIndexValue()) {
                if(mUdooBluManager!= null) mUdooBluManager.readAnalog(mAddress, iReaderListener);
            } else {
                configAnalog(pin, new OnBluOperationResult<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if(mUdooBluManager!= null) mUdooBluManager.readAnalog(mAddress, iReaderListener);
                    }

                    @Override
                    public void onError(UdooBluException runtimeException) {
                       if(iReaderListener != null) iReaderListener.onError(runtimeException);
                    }
                });
            }
        }
    }

    public void readAccelerometer(IReaderListener<byte[]> readerListener) {
        if(mUdooBluManager!= null) readSensor(mAddress, readerListener, SENSORS.ACC, UDOOBLESensor.ACCELEROMETER);
    }

    public void readGyroscope(IReaderListener<byte[]> readerListener) {
        if(mUdooBluManager!= null) readSensor(mAddress, readerListener, SENSORS.GYRO, UDOOBLESensor.GYROSCOPE);
    }

    public void readMagnetometer(IReaderListener<byte[]> readerListener) {
        if(mUdooBluManager!= null) readSensor(mAddress, readerListener, SENSORS.MAGN, UDOOBLESensor.MAGNETOMETER);
    }

    public void readBarometer(IReaderListener<byte[]> readerListener) {
        if(mUdooBluManager!= null) readSensor(mAddress, readerListener, SENSORS.BAR, UDOOBLESensor.BAROMETER_P);
    }

    public void readTemperature(IReaderListener<byte[]> onCharacteristicsListener) {
        if(mUdooBluManager!= null) readSensor(mAddress, onCharacteristicsListener, SENSORS.TEMP, UDOOBLESensor.TEMPERATURE);
    }

    public void readHumidity(IReaderListener<byte[]> onCharacteristicsListener) {
        if(mUdooBluManager!= null) readSensor(mAddress, onCharacteristicsListener, SENSORS.HUM, UDOOBLESensor.HUMIDITY);
    }

    public void readAmbientLight(IReaderListener<byte[]> onCharacteristicsListener) {
        if(mUdooBluManager!= null) readSensor(mAddress, onCharacteristicsListener, SENSORS.AMB_LIG, UDOOBLESensor.AMBIENT_LIGHT);
    }

    public void subscribeNotificationAccelerometer(INotificationListener<byte[]> notificationListener) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.ACC, UDOOBLESensor.ACCELEROMETER, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationAccelerometer(INotificationListener<byte[]> notificationListener, int period) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.ACC, UDOOBLESensor.ACCELEROMETER, period, notificationListener);
    }

    public void unSubscribeNotificationAccelerometer(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationAccelerometer(mAddress, operationResult);
    }

    public void subscribeNotificationGyroscope(INotificationListener<byte[]> notificationListener) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.GYRO, UDOOBLESensor.GYROSCOPE, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationGyroscope(INotificationListener<byte[]> notificationListener, int period) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.GYRO, UDOOBLESensor.GYROSCOPE, period, notificationListener);
    }

    public void unSubscribeNotificationGyroscope(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationGyroscope(mAddress, operationResult);
    }

    public void subscribeNotificationMagnetometer(INotificationListener<byte[]> notificationListener) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.MAGN, UDOOBLESensor.MAGNETOMETER, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationMagnetometer(INotificationListener<byte[]> notificationListener, int period) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.MAGN, UDOOBLESensor.MAGNETOMETER, period, notificationListener);
    }

    public void unSubscribeNotificationMagnetometer(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationMagnetometer(mAddress, operationResult);
    }

    public void subscribeNotificationBarometer(INotificationListener<byte[]> notificationListener) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.BAR, UDOOBLESensor.BAROMETER_P, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationBarometer(INotificationListener<byte[]> notificationListener, int period) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.BAR, UDOOBLESensor.BAROMETER_P, period, notificationListener);
    }

    public void unSubscribeNotificationBarometer(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationBarometer(mAddress, operationResult);
    }

    public void subscribeNotificationTemperature(INotificationListener<byte[]> notificationListener) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.TEMP, UDOOBLESensor.TEMPERATURE, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationTemperature(INotificationListener<byte[]> notificationListener, int period) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.TEMP, UDOOBLESensor.TEMPERATURE, period, notificationListener);
    }

    public void unSubscribeNotificationTemperature(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationTemperature(mAddress, operationResult);
    }

    public void subscribeNotificationHumidity(INotificationListener<byte[]> notificationListener) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.HUM, UDOOBLESensor.HUMIDITY, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationHumidity(INotificationListener<byte[]> notificationListener, int period) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.HUM, UDOOBLESensor.HUMIDITY, period, notificationListener);
    }

    public void unSubscribeNotificationHumidity(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationHumidity(mAddress, operationResult);
    }

    public void subscribeNotificationAmbientLight(INotificationListener<byte[]> notificationListener) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.HUM, UDOOBLESensor.HUMIDITY, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationAmbientLight(INotificationListener<byte[]> notificationListener, int period) {
        if(mUdooBluManager!= null) setNotification(mAddress, SENSORS.HUM, UDOOBLESensor.HUMIDITY, period, notificationListener);
    }

    public void unSubscribeNotificationAmbientLight(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationAmbientLight(mAddress, operationResult);
    }

    public void subscribeNotificationAnalog(IOPin.IOPIN_PIN pin, final INotificationListener<byte[]> notificationListener) {
        subscribeNotificationAnalog(pin, notificationListener, Constant.NOTIFICATIONS_PERIOD);
    }

    public void subscribeNotificationAnalog(IOPin.IOPIN_PIN pin, final INotificationListener<byte[]> notificationListener,final int period) {
        final IOPin ioPin = IOPin.Builder(pin, IOPin.IOPIN_MODE.ANALOG);
        if (iOPinVerifier(ioPin)) {
            if (indexAnalogConfig == ioPin.getIndexValue()) {
                if(mUdooBluManager!= null) mUdooBluManager.setNotification(mAddress, UDOOBLESensor.IOPIN_ANALOG, period, notificationListener);
            } else {
                configAnalog(pin, new OnBluOperationResult<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if(mUdooBluManager!= null) mUdooBluManager.setNotification(mAddress, UDOOBLESensor.IOPIN_ANALOG, period, notificationListener);
                    }

                    @Override
                    public void onError(UdooBluException runtimeException) {
                        if(notificationListener != null) notificationListener.onError(runtimeException);
                    }
                });
            }
        }
    }

    public void unSubscribeNotificationAnalog(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationAnalog(mAddress,operationResult);
    }

    public void setPinAnalogPwmIndex(IOPin ioPin, OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.setPinAnalogPwmIndex(mAddress, ioPin, operationResult);
    }

    /*notified on pin change*/
    public void subscribeNotificationDigital(final INotificationListener<byte[]> notificationListener, final IOPin... pins) {
        if (iOPinVerifier(IOPin.IOPIN_MODE.DIGITAL_INPUT, pins)) {
            if(mUdooBluManager!= null) mUdooBluManager.subscribeNotificationDigital(mAddress, notificationListener);
        } else {
            iOPinModeBuilder(IOPin.IOPIN_MODE.DIGITAL_INPUT, pins);
            setIoPinMode(new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    setLocaliOPinConfig(IOPin.IOPIN_MODE.DIGITAL_INPUT, pins);
                    if(mUdooBluManager!= null) mUdooBluManager.subscribeNotificationDigital(mAddress, notificationListener);
                }
                @Override
                public void onError(UdooBluException runtimeException) {
                    if (runtimeException != null && notificationListener != null)
                        notificationListener.onError(new UdooBluException(UdooBluException.BLU_NOTIFICATION_ERROR));
                }
            }, pins);
        }
    }

    public void unSubscribeNotificationDigital(OnBluOperationResult<Boolean> operationResult) {
        if(mUdooBluManager!= null) mUdooBluManager.unSubscribeNotificationDigital(mAddress, operationResult);
    }

    private void readSensor(final String address, final IReaderListener<byte[]> readerListener, final SENSORS sensor, final UDOOBLESensor udoobleSensor) {
        int sensVer = sensorVerifier(sensor);
        if (sensVer == 1) {
            mUdooBluManager.readSensor(address, readerListener, sensor, udoobleSensor);
        } else if (sensVer == 0) {
            mUdooBluManager.enableSensor(address, udoobleSensor, true, new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    if (aBoolean) {
                        sensorsEnabled[sensor.ordinal()] = true;
                        readSensor(address, readerListener, sensor, udoobleSensor);
                    } else {
                        if (readerListener != null)
                            readerListener.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));
                    }
                }
                @Override
                public void onError(UdooBluException runtimeException) {
                    if (readerListener != null)
                        readerListener.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));
                }
            });
        } else {
            if (readerListener != null)
                readerListener.onError(new UdooBluException(UdooBluException.BLU_SENSOR_NOT_FOUND));
        }
    }

    private void setNotification(final String address, final SENSORS sensor, final UDOOBLESensor udoobleSensor, final int period, final INotificationListener<byte[]> iNotificationListener) {
        int sensVer = sensorVerifier(sensor);
        if (sensVer == 1) {
            mUdooBluManager.setNotification(address, udoobleSensor, period, iNotificationListener);
        } else if (sensVer == 0) {
            mUdooBluManager.enableSensor(address, udoobleSensor, true, new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    if (aBoolean) {
                        sensorsEnabled[sensor.ordinal()] = true;
                        setNotification(address, sensor, udoobleSensor, period,iNotificationListener);
                    } else {
                        if (iNotificationListener != null)
                            iNotificationListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                    }
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    if (iNotificationListener != null)
                        iNotificationListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                }
            });
        } else {
            if (iNotificationListener != null)
                iNotificationListener.onError(new UdooBluException(UdooBluException.BLU_SENSOR_NOT_FOUND));
        }
    }

    private void configAnalog(final IOPin.IOPIN_PIN pin, final OnBluOperationResult<Boolean> operationResult){
        final IOPin ioPin = IOPin.Builder(pin, IOPin.IOPIN_MODE.ANALOG);
        if (iOPinVerifier(ioPin)) {
            if (indexAnalogConfig == ioPin.getIndexValue()) {
                if (operationResult != null)
                    operationResult.onSuccess(true);
            } else {
                mUdooBluManager.setPinAnalogOrPwmIndex(mAddress, IOPin.Builder(pin, IOPin.IOPIN_INDEX_VALUE.ANALOG), new OnBluOperationResult<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if (aBoolean) {
                            indexAnalogConfig = ioPin.getIndexValue();
                            if (operationResult != null)
                                operationResult.onSuccess(true);
                        } else if (operationResult != null) {
                            operationResult.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                        }
                    }

                    @Override
                    public void onError(UdooBluException runtimeException) {
                        if (runtimeException != null && operationResult != null)
                            operationResult.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));
                    }

                });
            }
        } else {
            iOPinModeBuilder(IOPin.IOPIN_MODE.ANALOG, ioPin);
            setIoPinMode(new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    if (aBoolean) {
                        configAnalog(pin, operationResult);
                    } else if (operationResult != null) {
                        operationResult.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                    }
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    if (runtimeException != null && operationResult != null)
                        operationResult.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));
                }
            }, ioPin);
        }
    }

    private void setLocaliOPinConfig(IOPin.IOPIN_MODE mode, IOPin... ioPins) {
        for (int i = 0; i < iOPinConfig.length && i < ioPins.length; i++)
            iOPinConfig[ioPins[i].pin.ordinal()] = (byte) mode.ordinal();
    }

    private boolean iOPinVerifier(IOPin.IOPIN_MODE mode, IOPin... ioPins) {
        boolean configured = true;
        for (int i = 0; i < iOPinConfig.length && i < ioPins.length; i++) {
            if (iOPinConfig[ioPins[i].pin.ordinal()] != mode.ordinal())
                configured = false;
        }
        return configured;
    }

    private boolean iOPinVerifier(IOPin... ioPins) {
        boolean configured = true;
        IOPin ioPin;
        int len = ioPins.length;
        for (int i = 0; i < len; i++) {
            ioPin = ioPins[i];
            for (int j = 0; j < mIOPins.length; j++) {
                if (mIOPins[j] != null && ioPin.pin == mIOPins[j].pin && ioPin.mode != mIOPins[j].mode) {
                    configured = false;
                    break;
                }
            }
        }
        return configured;
    }

    private void iOPinModeBuilder(IOPin.IOPIN_MODE mode, IOPin... ioPins) {
        for (int i = 0; i < ioPins.length; i++) {
            ioPins[i].mode = mode;
        }
    }

    private IOPin[] mergeWithLocalIOPinConfig(IOPin... ioPins) {
        IOPin[] tmpIOPins = mIOPins.clone();
        IOPin ioPin;
        int len = ioPins.length;
        if (len > 8)
            len = 8;

        for (int i = 0; i < len; i++) {
            ioPin = ioPins[i];
            boolean found = false;
            for (int j = 0; !found && j < tmpIOPins.length; j++) {
                if (tmpIOPins[j] != null && ioPin.pin == tmpIOPins[j].pin) {
                    tmpIOPins[j] = ioPin;
                    found = true;
                }
            }
            if (!found) {
                for (int j = 0; j < tmpIOPins.length; j++) {
                    if (tmpIOPins[j] == null) {
                        tmpIOPins[j] = ioPin;
                        j = tmpIOPins.length;
                    }
                }
            }
        }
        return tmpIOPins;
    }


    /***
     * @param sensors
     * @return -1 sensor not detected, 0 sensor detected,  1 sensor detected and enabled
     */
    private int sensorVerifier(SENSORS sensors) {
        return sensorsDetected[sensors.ordinal()] ? (sensorsEnabled[sensors.ordinal()] ? 1 : 0) : -1;
    }
}
