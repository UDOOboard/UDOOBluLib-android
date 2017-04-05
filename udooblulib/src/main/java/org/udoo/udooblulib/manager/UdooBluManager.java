package org.udoo.udooblulib.manager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.udoo.udooblulib.BuildConfig;
import org.udoo.udooblulib.UdooBlu;
import org.udoo.udooblulib.exceptions.UdooBluException;
import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.interfaces.IBluManagerCallback;
import org.udoo.udooblulib.interfaces.INotificationListener;
import org.udoo.udooblulib.interfaces.IReaderListener;
import org.udoo.udooblulib.interfaces.OnBluOperationResult;
import org.udoo.udooblulib.interfaces.OnResult;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.scan.BluScanCallBack;
import org.udoo.udooblulib.sensor.Constant;
import org.udoo.udooblulib.sensor.TIUUID;
import org.udoo.udooblulib.sensor.UDOOBLE;
import org.udoo.udooblulib.sensor.UDOOBLESensor;
import org.udoo.udooblulib.service.UdooBluService;
import org.udoo.udooblulib.utils.BitUtility;
import org.udoo.udooblulib.utils.SeqObserverQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by harlem88 on 24/03/16.
 */

public class UdooBluManager {
    private boolean mBound;
    private UdooBluService mUdooBluService;
    private HashMap<String, OnBluOperationResult<Boolean>> mOnResultMap;
    private HashMap<String, IBleDeviceListener> mDeviceListenerMap;
    private HashMap<String, IReaderListener<byte[]>> mIReaderListenerMap;
    private HashMap<String, INotificationListener<byte[]>> mINotificationListenerMap;
    private boolean isBluManagerReady;
    private Handler mHandler;
    private boolean mScanning;
    private String TAG = "BluManager";
    private IBluManagerCallback mIBluManagerCallback;
    private BlockingQueue<Callable> voidBlockingQueue = new LinkedBlockingQueue<>(10);
    private SeqObserverQueue seqObserverQueue = new SeqObserverQueue<>(voidBlockingQueue, 300);
    private boolean mIsBindService;
    private static final String BLU_FILE = "blu_prefs.xml";
    private UdooBluManager mUdooBluManager;
    private static UdooBluManager sUdooBluManager;
    private HashMap<String, UdooBlu> mUdooBluConnected;
    public enum SENSORS {ACC ,MAGN, GYRO,TEMP,BAR,HUM, AMB_LIG, RES}

    public static void GetUdooBluManager(Context context, IBluManagerCallback bluManagerCallback){
        if(sUdooBluManager == null) sUdooBluManager = new UdooBluManager(context);
        sUdooBluManager.setIBluManagerCallback(bluManagerCallback);
    }

    private UdooBluManager(Context context) {
        mUdooBluManager = this;
        init(context);
    }

    public void init(Context context) {
        mDeviceListenerMap = new HashMap<>();
        mIReaderListenerMap = new HashMap<>();
        mINotificationListenerMap = new HashMap<>();
        mUdooBluConnected = new HashMap<>();
        mOnResultMap = new HashMap<>();
        mIsBindService = context.bindService(new Intent(context, UdooBluService.class), mConnection, Context.BIND_AUTO_CREATE);
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        context.registerReceiver(mGattBoundReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mHandler = new Handler(Looper.getMainLooper());
        seqObserverQueue.run();
    }

    public void setIBluManagerCallback(IBluManagerCallback iBluManagerCallback) {
        mIBluManagerCallback = iBluManagerCallback;
        if (isBluManagerReady && mIBluManagerCallback != null)
            mIBluManagerCallback.onBluManagerReady(sUdooBluManager);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            UdooBluService.LocalBinder binder = (UdooBluService.LocalBinder) service;
            mUdooBluService = binder.getService();
            mBound = true;
            if (mUdooBluService.initialize()) {
                isBluManagerReady = true;
                if (mIBluManagerCallback != null)
                    mIBluManagerCallback.onBluManagerReady(sUdooBluManager);
            } else {
                isBluManagerReady = false;
            }

            if (BuildConfig.DEBUG)
                Log.i(TAG, "connected to udooBluService");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (BuildConfig.DEBUG)
                Log.i(TAG, "failed to udooBluService");
            mBound = false;
        }
    };

    public void scanLeDevice(boolean enable, BluScanCallBack scanCallback) {
        if (mIsBindService && isBluManagerReady) {
            mUdooBluService.scanLeDevice(enable, scanCallback);
        } else if (scanCallback != null)
            scanCallback.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
    }

    public void connect(final String address, IBleDeviceListener iBleDeviceListener) {
        if (mIsBindService && isBluManagerReady) {
            mUdooBluService.connect(address, iBleDeviceListener);
            setIBleDeviceListener(address, iBleDeviceListener);
        } else if (iBleDeviceListener != null)
            iBleDeviceListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
    }

    public void setIBleDeviceListener(String address, IBleDeviceListener iBleDeviceListener) {
        mDeviceListenerMap.put(address, iBleDeviceListener);
    }

    public void writeLed(String address, int color, boolean enable) {
        writeLed(address, color, enable ? Constant.LED_ON : Constant.LED_OFF);
    }

    public void blinkLed(String address, int color, boolean blink) {
        writeLed(address, color, blink ? Constant.BLINK_ON : Constant.LED_OFF);
    }

    private void writeLed(String address, int color, byte func) {
        BluetoothGattService serv = null;
        BluetoothGattCharacteristic charac = null;
        serv = mUdooBluService.getService(address, UDOOBLE.UUID_LED_SERV);
        if (serv != null) {
            switch (color) {
                case Constant.GREEN_LED:
                    charac = serv.getCharacteristic(UDOOBLE.UUID_LED_GREEN);
                    break;
                case Constant.YELLOW_LED:
                    charac = serv.getCharacteristic(UDOOBLE.UUID_LED_YELLOW);
                    break;
                case Constant.RED_LED:
                    charac = serv.getCharacteristic(UDOOBLE.UUID_LED_RED);
                    break;
            }

            byte[] msg = new byte[2];
            msg[0] = func;
            msg[1] = (byte) 0x03;

            mUdooBluService.writeCharacteristic(address, charac, msg);
        }
    }

    private void detectSensors(String address, IReaderListener<byte[]> readerListener) {
        if (isBluManagerReady) {
            UUID servUuid = UDOOBLE.UUID_SENSORS_SERV;
            UUID dataUuid = UDOOBLE.UUID_SENSOR_DATA;

            BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);

                mUdooBluService.readCharacteristic(address, charac);
                mIReaderListenerMap.put(address + charac.getUuid().toString(), readerListener);
            } else {
                if (readerListener != null)
                    readerListener.onError(new UdooBluException(UdooBluException.BLU_SENSOR_NOT_FOUND));
            }

        } else {
            if (BuildConfig.DEBUG)
                Log.i(TAG, "BluManager not ready");

            if (readerListener != null)
                readerListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }
//    public String connects(IBleDeviceListener iBleDeviceListener) {
//        String address = "";
//        if (isBluManagerReady) {
//            address = mUdooBluService.connectWithBounded();
//            if (address != null && address.length() > 0)
//                mDeviceListenerMap.put(address, iBleDeviceListener);
//        } else if (BuildConfig.DEBUG)
//            Log.i(TAG, "BluManager not ready");
//        return address;
//    }

    public void enableSensor(final String address, final UDOOBLESensor sensor, final boolean enable,final OnBluOperationResult<Boolean> operationResult) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (sensor != null) {
                        UUID servUuid = sensor.getService();
                        UUID confUuid = sensor.getConfig();
                        BluetoothGattService serv = null;
                        BluetoothGattCharacteristic charac = null;

                        byte[] value = new byte[1];
                        try {
                            serv = mUdooBluService.getService(address, servUuid);
                            charac = serv.getCharacteristic(confUuid);
                            value[0] = enable ? sensor.getEnableSensorCode()
                                    : UDOOBLESensor.DISABLE_SENSOR_CODE;
                            mOnResultMap.put(address, operationResult);
                            mUdooBluService.writeCharacteristic(address, charac, value);
                        } catch (Exception e) {
                            if (operationResult != null)
                                operationResult.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                            if (BuildConfig.DEBUG)
                                Log.e(TAG, "error enableSensor(), service uuid: " + servUuid.toString());
                        }
                    }
                    return null;
                }
            });
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
    }

    private void setNotification(final String address, final UDOOBLESensor udoobleSensor, final INotificationListener<byte[]> iNotificationListener) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    UUID servUuid = udoobleSensor.getService();
                    UUID dataUuid = udoobleSensor.getData();
                    BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                        mINotificationListenerMap.put(address + charac.getUuid().toString(), iNotificationListener);
                        mUdooBluService.setCharacteristicNotification(address, charac, true);
                        Log.i(TAG, "setNotification: ");
                    } else if (iNotificationListener != null)
                        iNotificationListener.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                    return null;
                }
            });
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
    }

    /**
     * @param period in ms (1 ms to 6000ms)
     */
    private void setNotificationPeriod(final String address, final UDOOBLESensor udoobleSensor,final int period,final OnBluOperationResult<Boolean> operationResult) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    UUID servUuid = udoobleSensor.getService();
                    UUID dataUuid = UDOOBLE.UUID_NOTIFICATION_PERI;
                    BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

                    if (serv != null) {
                        //Value unit in 10ms , is unsigned integer 16 bit. > 0 and < 6000 (6000 -> 60 s)
                        int tmpPeriod = period / 10;
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                        byte[] msg = new byte[2];
                        byte value[] = BitUtility.To2Bytes(tmpPeriod);
                        msg[1] = value[0];
                        msg[0] = value[1];

                        mOnResultMap.put(address, operationResult);
                        mUdooBluService.writeCharacteristic(address, charac, msg);
                        Log.i(TAG, "setNotificationPeriod: ");
                    } else if (operationResult != null)
                        operationResult.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));

                    return null;
                }
            });
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
    }

    public void setNotification(final String address, final UDOOBLESensor udoobleSensor, final int period, final INotificationListener<byte[]> iNotificationListener) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (period != 0) {
                        setNotificationPeriod(address, udoobleSensor, period, new OnBluOperationResult<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                if (aBoolean)
                                    setNotification(address, udoobleSensor, iNotificationListener);
                                else if (iNotificationListener != null)
                                    iNotificationListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_PERIOD_NOTIFICATION_ERROR));

                                mOnResultMap.remove(address);
                            }

                            @Override
                            public void onError(UdooBluException runtimeException) {
                                if (iNotificationListener != null)
                                    iNotificationListener.onError(runtimeException);

                                mOnResultMap.remove(address);
                            }
                        });
                    } else setNotification(address, udoobleSensor, iNotificationListener);
                    return null;
                }
            });
        } else {
            if (BuildConfig.DEBUG)
                Log.i(TAG, "BluManager not ready");

            if (iNotificationListener != null)
                iNotificationListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

    private void addOperation(Callable<Void> operation) {
        try {
            voidBlockingQueue.put(operation);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setIoPinMode(final String address, final OnBluOperationResult<Boolean> onResultListener, final IOPin... ioPins) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    UUID service, characteristic;
                    byte msg[] = new byte[2];
                    service = UDOOBLE.UUID_IOPIN_SERV;
                    characteristic = UDOOBLE.UUID_IOPIN_PIN_MODE;
                    IOPin ioPin;

                    int len = ioPins.length;

                    for (int i = 0; i < len; i++) {
                        ioPin = ioPins[i];
                        if (ioPin != null) {
                            byte mode = ioPin.getPinMode();
                            int shift = 0;
                            switch (ioPin.pin) {
                                case D7:
                                case A3:
                                    shift = 6;
                                    break;
                                case D6:
                                case A2:
                                    shift = 4;
                                    break;
                                case A5:
                                case A1:
                                    shift = 2;
                                    break;
                                case A4:
                                case A0:
                                    shift = 0;
                                    break;
                            }
                            byte value = (byte) ((mode << shift) & 0xff);
                            int idx = ioPin.getPinValue() >= 4 ? 1 : 0;
                            msg[idx] = (byte) (msg[idx] | value);
                        }
                    }
                    Log.i(TAG, "call: set pin mode");
                    BitUtility.LogBinValue(msg, false);

                    BluetoothGattService serv = mUdooBluService.getService(address, service);
                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                        mOnResultMap.put(address, onResultListener);
                        mUdooBluService.writeCharacteristic(address, charac, msg);
                        if (BuildConfig.DEBUG)
                            BitUtility.LogBinValue(msg, false);
                    }
                    return null;
                }
            });
        } else if (onResultListener != null) {
            onResultListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

    public void readDigital(final String address, final IReaderListener<byte[]> readerListener) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
                    UUID dataUuid = UDOOBLE.UUID_IOPIN_DIGITAL_DATA;
                    BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                        mUdooBluService.readCharacteristic(address, charac);
                        mIReaderListenerMap.put(address + charac.getUuid().toString(), readerListener);
                    } else {
                        if (readerListener != null)
                            readerListener.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                    }
                    return null;
                }
            });
        } else {
            if (BuildConfig.DEBUG)
                Log.i(TAG, "BluManager not ready");

            if (readerListener != null)
                readerListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

    public void setPinAnalogOrPwmIndex(final String address, final IOPin ioPin, final OnBluOperationResult<Boolean> onResultListener) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    UUID service, characteristic;
                    byte[] msg;

                    service = UDOOBLE.UUID_IOPIN_SERV;
                    characteristic = UDOOBLE.UUID_IOPIN_PWM_ANALOG_INDEX;

                    msg = new byte[1];
                    msg[0] = ioPin.getIndexValue();

                    BluetoothGattService serv = mUdooBluService.getService(address, service);
                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                        mOnResultMap.put(address, onResultListener);

                        if (BuildConfig.DEBUG)
                            BitUtility.LogBinValue(msg, false);

                        mUdooBluService.writeCharacteristic(address, charac, msg);
                    } else if (onResultListener != null)
                        onResultListener.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));

                    return null;
                }
            });
        } else if (onResultListener != null) {
            onResultListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

//    @Override
//    public void readAnalog(final String address, final IOPin.IOPIN_PIN pin, final IReaderListener<byte[]> iReaderListener) {
//        addOperation(new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//                configAnalog(address, pin, new OnBluOperationResult<Boolean>() {
//                    @Override
//                    public void onSuccess(Boolean aBoolean) {
//                        if (aBoolean)
//                            readAnalog(address, iReaderListener);
//                        else if (iReaderListener != null)
//                            iReaderListener.onError(new UdooBluException(UdooBluException.BLU_NOTIFICATION_ERROR));
//                        mOnResultMap.remove(address);
//                    }
//
//                    @Override
//                    public void onError(UdooBluException runtimeException) {
//                        if (iReaderListener != null)
//                            iReaderListener.onError(runtimeException);
//
//                        mOnResultMap.remove(address);
//                    }
//                });
//                return null;
//            }
//        });
//    }

    public void readAnalog(final String address, final IReaderListener<byte[]> iReaderListener) {
        addOperation(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
                UUID dataUuid = UDOOBLE.UUID_IOPIN_ANALOG_READ;

                BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

                if (serv != null) {
                    BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                    mUdooBluService.readCharacteristic(address, charac);
                    mIReaderListenerMap.put(address + charac.getUuid().toString(), iReaderListener);
                } else {
                    if (iReaderListener != null)
                        iReaderListener.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                }
                return null;
            }
        });
    }

    public void configAnalog(final String address, final IOPin.IOPIN_PIN pin, final OnBluOperationResult<Boolean> operationResult) {
        if (isBluManagerReady) {
//            addOperation(new Callable<Void>() {
//                @Override
//                public Void call() throws Exception {
//                    final IOPin ioPin = IOPin.Builder(pin, IOPin.IOPIN_MODE.ANALOG);
//                    if (iOPinVerifier(ioPin)) {
//                        if (indexAnalogConfig == ioPin.getIndexValue()) {
//                            if (operationResult != null)
//                                operationResult.onSuccess(true);
//                        } else {
//                            setPinAnalogOrPwmIndex(address, IOPin.Builder(pin, IOPin.IOPIN_INDEX_VALUE.ANALOG), new OnBluOperationResult<Boolean>() {
//                                @Override
//                                public void onSuccess(Boolean aBoolean) {
//                                    if (aBoolean) {
//                                        indexAnalogConfig = ioPin.getIndexValue();
//                                        if (operationResult != null)
//                                            operationResult.onSuccess(true);
//                                    } else if (operationResult != null) {
//                                        operationResult.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
//                                    }
//                                }
//
//                                @Override
//                                public void onError(UdooBluException runtimeException) {
//                                    if (runtimeException != null && operationResult != null)
//                                        operationResult.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));
//                                }
//
//                            });
//                        }
//                    } else {
//                        iOPinModeBuilder(IOPin.IOPIN_MODE.ANALOG, ioPin);
//                        setIoPinMode(address, new OnBluOperationResult<Boolean>() {
//                            @Override
//                            public void onSuccess(Boolean aBoolean) {
//                                if (aBoolean) {
////                                    setLocaliOPinConfig(IOPin.IOPIN_MODE.ANALOG, ioPin);
//                                    configAnalog(address, pin, operationResult);
//                                } else if (operationResult != null) {
//                                    operationResult.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
//                                }
//                            }
//
//                            @Override
//                            public void onError(UdooBluException runtimeException) {
//                                if (runtimeException != null && operationResult != null)
//                                    operationResult.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));
//                            }
//                        }, ioPin);
//                    }
//                    return null;
//                }
//            });
//
//        } else {
//
//            if (operationResult != null)
//                operationResult.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
//
//            if (BuildConfig.DEBUG)
//                Log.i(TAG, "BluManager not ready");
        }
    }

    public void writePwm(final String address, final int freq, final int dutyCycle, final OnBluOperationResult<Boolean> onResultListener) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
                    UUID dataUuid = UDOOBLE.UUID_IOPIN_PWM_CONFIG;
                    byte msg[] = new byte[5];
                    if (freq >= 3 && freq <= 24000000) {
                        byte freqs[] = BitUtility.ToBytes(freq);
                        msg[3] = freqs[0];
                        msg[2] = freqs[1];
                        msg[1] = freqs[2];
                        msg[0] = freqs[3];

                        msg[4] = (byte) (dutyCycle & 0xff);

                        BluetoothGattService serv = mUdooBluService.getService(address, servUuid);
                        BitUtility.LogBinValue(msg, false);
                        if (serv != null) {
                            BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                            mOnResultMap.put(address, onResultListener);
                            mUdooBluService.writeCharacteristic(address, charac, msg);
                        } else {
                            if (onResultListener != null)
                                onResultListener.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                        }
                    } else if (onResultListener != null)
                        onResultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                    return null;
                }
            });

        } else {

            if (onResultListener != null)
                onResultListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));

            if (BuildConfig.DEBUG)
                Log.i(TAG, "BluManager not ready");
        }
    }

    public void writeDigital(final String address, final OnBluOperationResult<Boolean> onBluOperationResult, final IOPin... ioPins) {
        addOperation(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                    if (isBluManagerReady) {
                        byte[] msg;
                        UUID service = UDOOBLE.UUID_IOPIN_SERV, characteristic;
                        characteristic = UDOOBLE.UUID_IOPIN_DIGITAL_DATA;

                        msg = new byte[1];

                        int len = ioPins.length;
                        if (len > 8)
                            len = 8;

                        IOPin ioPin;
                        for (int i = 0; i < len; i++) {
                            ioPin = ioPins[i];
                            int shift = 0;
                            switch (ioPin.pin) {
                                case D7:
                                    shift = 7;
                                    break;
                                case D6:
                                    shift = 6;
                                    break;
                                case A5:
                                    shift = 5;
                                    break;
                                case A4:
                                    shift = 4;
                                    break;
                                case A3:
                                    shift = 3;
                                    break;
                                case A2:
                                    shift = 2;
                                    break;
                                case A1:
                                    shift = 1;
                                    break;
                                case A0:
                                    shift = 0;
                                    break;
                            }
                            byte value = (byte) ((ioPin.getShortDigitalValue() << shift) & 0xff);
                            msg[0] = (byte) (msg[0] | value);
                        }

                        Log.i(TAG, "call: write difital");
                        BitUtility.LogBinValue(msg, false);


                        BluetoothGattService serv = mUdooBluService.getService(address, service);
                        if (serv != null) {
                            BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                            mOnResultMap.put(address, onBluOperationResult);
                            mUdooBluService.writeCharacteristic(address, charac, msg);
                            if (BuildConfig.DEBUG)
                                BitUtility.LogBinValue(msg, false);
                        }
                    } else if (onBluOperationResult != null) {
                        onBluOperationResult.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
                    }
                return null;
            }
        });
    }

    private void unSubscribeNotification(final String address, final UDOOBLESensor udoobleSensor, final OnBluOperationResult<Boolean> operationResult) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    UUID servUuid = udoobleSensor.getService();
                    UUID dataUuid = udoobleSensor.getData();
                    BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

                    Log.i(TAG, "enableNotifications service " + servUuid.toString() + " is null: " + (serv == null));

                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                        String keySearch = address + charac.getUuid().toString();

                        if (mINotificationListenerMap.containsKey(keySearch))
                            mINotificationListenerMap.remove(keySearch);

                        mOnResultMap.put(address, operationResult);
                        mUdooBluService.setCharacteristicNotification(address, charac, false);
                    } else if (operationResult != null)
                        operationResult.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                    return null;
                }
            });
        } else {
            if (BuildConfig.DEBUG)
                Log.i(TAG, "BluManager not ready");

            if (operationResult != null)
                operationResult.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

    public void readSensor(final String address, final IReaderListener<byte[]> readerListener, final SENSORS sensor, final UDOOBLESensor udoobleSensor) {
        if (isBluManagerReady) {
            addOperation(new Callable<Void>() {
                @Override
                public Void call() throws Exception {

                    UUID servUuid = udoobleSensor.getService();
                    UUID dataUuid = udoobleSensor.getData();
                    BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                        mUdooBluService.readCharacteristic(address, charac);
                        mIReaderListenerMap.put(address + charac.getUuid().toString(), readerListener);
                    } else {
                        if (readerListener != null)
                            readerListener.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                    }
                    return null;
                }
            });
        } else {
            if (BuildConfig.DEBUG)
                Log.i(TAG, "BluManager not ready");

            if (readerListener != null)
                readerListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

    public void readAccelerometer(String address, IReaderListener<byte[]> readerListener) {
        readSensor(address, readerListener, SENSORS.ACC, UDOOBLESensor.ACCELEROMETER);
    }

    public void readGyroscope(String address, IReaderListener<byte[]> readerListener) {
        readSensor(address, readerListener, SENSORS.GYRO, UDOOBLESensor.GYROSCOPE);
    }

    public void readMagnetometer(String address, IReaderListener<byte[]> readerListener) {
        readSensor(address, readerListener, SENSORS.MAGN, UDOOBLESensor.MAGNETOMETER);
    }

    public void readBarometer(String address, IReaderListener<byte[]> readerListener) {
        readSensor(address, readerListener, SENSORS.BAR, UDOOBLESensor.BAROMETER_P);
    }

    public void readTemperature(String address, IReaderListener<byte[]> readerListener) {
        readSensor(address, readerListener, SENSORS.TEMP, UDOOBLESensor.TEMPERATURE);
    }


    public void readHumidity(String address, IReaderListener<byte[]> readerListener) {
        readSensor(address, readerListener, SENSORS.HUM, UDOOBLESensor.HUMIDITY);
    }

    public void readAmbientLight(String address, IReaderListener<byte[]> readerListener) {
        readSensor(address, readerListener, SENSORS.AMB_LIG, UDOOBLESensor.AMBIENT_LIGHT);
    }

    public void subscribeNotificationAccelerometer(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.ACCELEROMETER, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void unSubscribeNotificationAccelerometer(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.ACCELEROMETER, operationResult);
    }

    public void subscribeNotificationAccelerometer(String address, INotificationListener<byte[]> notificationListener, int period) {
        setNotification(address, UDOOBLESensor.ACCELEROMETER, period, notificationListener);
    }


    public void subscribeNotificationGyroscope(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.GYROSCOPE, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationGyroscope(String address, INotificationListener<byte[]> notificationListener, int period) {
        setNotification(address, UDOOBLESensor.GYROSCOPE, period, notificationListener);
    }

    public void unSubscribeNotificationGyroscope(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.GYROSCOPE, operationResult);
    }

    public void subscribeNotificationMagnetometer(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.MAGNETOMETER, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationMagnetometer(String address, INotificationListener<byte[]> notificationListener, int period) {
        setNotification(address, UDOOBLESensor.MAGNETOMETER, period, notificationListener);
    }

    public void unSubscribeNotificationMagnetometer(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.MAGNETOMETER, operationResult);
    }


    public void subscribeNotificationBarometer(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.BAROMETER_P, Constant.NOTIFICATIONS_PERIOD, notificationListener);

    }

    public void subscribeNotificationBarometer(String address, INotificationListener<byte[]> notificationListener, int period) {
        setNotification(address, UDOOBLESensor.BAROMETER_P, period, notificationListener);
    }

    public void unSubscribeNotificationBarometer(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.BAROMETER_P, operationResult);
    }

    public void subscribeNotificationTemperature(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.TEMPERATURE, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationTemperature(String address, INotificationListener<byte[]> notificationListener, int period) {
        setNotification(address, UDOOBLESensor.TEMPERATURE, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void unSubscribeNotificationTemperature(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.TEMPERATURE, operationResult);
    }


    public void subscribeNotificationHumidity(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.HUMIDITY, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationHumidity(String address, INotificationListener<byte[]> notificationListener, int period) {
        setNotification(address, UDOOBLESensor.HUMIDITY, period, notificationListener);
    }

    public void unSubscribeNotificationHumidity(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.HUMIDITY, operationResult);
    }

    public void subscribeNotificationAmbientLight(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.AMBIENT_LIGHT, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationAmbientLight(String address, INotificationListener<byte[]> notificationListener, int period) {
        setNotification(address, UDOOBLESensor.AMBIENT_LIGHT, period, notificationListener);
    }

    public void unSubscribeNotificationAmbientLight(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.AMBIENT_LIGHT, operationResult);
    }

    public void subscribeNotificationAnalog(final String address, final INotificationListener<byte[]> notificationListener) {
        subscribeNotificationAnalog(address, Constant.NOTIFICATIONS_PERIOD, notificationListener);
    }

    public void subscribeNotificationAnalog(final String address, int interval, final INotificationListener<byte[]> notificationListener) {
        setNotificationPeriod(address, UDOOBLESensor.IOPIN_ANALOG, interval, new OnBluOperationResult<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (aBoolean) {
                    setNotification(address, UDOOBLESensor.IOPIN_ANALOG, notificationListener);
                } else if (notificationListener != null)
                    notificationListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_PERIOD_NOTIFICATION_ERROR));

                mOnResultMap.remove(address);
            }

            @Override
            public void onError(UdooBluException runtimeException) {
                if (notificationListener != null)
                    notificationListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_PERIOD_NOTIFICATION_ERROR));

                mOnResultMap.remove(address);
            }
        });
    }

    public void subscribeNotificationAnalog(final String address, final IOPin.IOPIN_PIN pin, final INotificationListener<byte[]> notificationListener, final int period) {
        configAnalog(address, pin, new OnBluOperationResult<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (aBoolean) {
                    setNotificationPeriod(address, UDOOBLESensor.IOPIN_ANALOG, period, new OnBluOperationResult<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            if (aBoolean) {
                                setNotification(address, UDOOBLESensor.IOPIN_ANALOG, notificationListener);
                            } else if (notificationListener != null)
                                notificationListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_PERIOD_NOTIFICATION_ERROR));

                            mOnResultMap.remove(address);
                        }

                        @Override
                        public void onError(UdooBluException runtimeException) {
                            if (notificationListener != null)
                                notificationListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_PERIOD_NOTIFICATION_ERROR));

                            mOnResultMap.remove(address);
                        }
                    });
                } else if (notificationListener != null)
                    notificationListener.onError(new UdooBluException(UdooBluException.BLU_NOTIFICATION_ERROR));

            }

            @Override
            public void onError(UdooBluException runtimeException) {
                if (notificationListener != null)
                    notificationListener.onError(runtimeException);
            }
        });

    }

    public void subscribeNotificationAnalog(final String address, IOPin.IOPIN_PIN pin, final INotificationListener<byte[]> notificationListener) {
        subscribeNotificationAnalog(address, pin, notificationListener, Constant.NOTIFICATIONS_PERIOD);
    }

    public void unSubscribeNotificationAnalog(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.IOPIN_ANALOG, operationResult);
    }

    public void setPinAnalogPwmIndex(String address, IOPin ioPin, OnBluOperationResult<Boolean> operationResult) {
        setPinAnalogOrPwmIndex(address, ioPin, operationResult);
    }

    public void subscribeNotificationDigital(String address, INotificationListener<byte[]> notificationListener) {
        setNotification(address, UDOOBLESensor.IOPIN_DIGITAL, notificationListener);
    }

    public void unSubscribeNotificationDigital(String address, OnBluOperationResult<Boolean> operationResult) {
        unSubscribeNotification(address, UDOOBLESensor.IOPIN_DIGITAL, operationResult);
    }


    public boolean pwmWrite(IOPin.IOPIN_PIN pin, int freq, int dutyCycle) {
        return false;
    }

    private final BroadcastReceiver mGattBoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {

                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (bondState == BluetoothDevice.BOND_BONDED) {
                    Log.i(TAG, "onReceive: bonded");
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    Log.i(TAG, "onReceive: not bonded");
                }
            }
        }

    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int status = intent.getIntExtra(UdooBluService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);
            byte[] value = intent.getByteArrayExtra(UdooBluService.EXTRA_DATA);
            String uuidStr = intent.getStringExtra(UdooBluService.EXTRA_UUID);
            final String address = intent.getStringExtra(UdooBluService.EXTRA_ADDRESS);

            if (UdooBluService.ACTION_GATT_CONNECTED.equals(action)) {
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "onReceive: connect");

                mUdooBluService.scanServices(address);
            } else if (UdooBluService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "onReceive: discover services");

                    if (mDeviceListenerMap.containsKey(address)) {
                        mUdooBluService.bond(address);
                        detectSensors(address, new IReaderListener<byte[]>() {
                            @Override
                            public void oRead(byte[] value) {
                                IBleDeviceListener iBleDeviceListener = mDeviceListenerMap.get(address);
                                boolean[] sensorsDetected = new boolean[8];
                                for (int i = 0; i < sensorsDetected.length; i++) {
                                    sensorsDetected[i] = (value[0] & (1 << i)) > 0;
                                }
                                if (iBleDeviceListener != null){
                                    UdooBlu udooBlu = new UdooBlu(address, sensorsDetected, mUdooBluManager);
                                    mUdooBluConnected.put(address, udooBlu);
                                    iBleDeviceListener.onDeviceConnected(udooBlu);
                                }
                            }

                            @Override
                            public void onError(UdooBluException e) {
                                IBleDeviceListener iBleDeviceListener = mDeviceListenerMap.get(address);
                                if (iBleDeviceListener != null)
                                    iBleDeviceListener.onError(e);
                            }
                        });
                    } else {
                        Toast.makeText(context, "Service discovery failed", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            } else if ((UdooBluService.ACTION_DATA_NOTIFY.equals(action) ||
                    UdooBluService.ACTION_DATA_WRITE.equals(action) ||
                    UdooBluService.ACTION_DATA_READ.equals(action))) {
                String keySearch = address + uuidStr;

                if (UdooBluService.ACTION_DATA_NOTIFY.equals(action)) {
                    // Notification
                    INotificationListener<byte[]> iNotificationListener = mINotificationListenerMap.get(keySearch);
                    if (iNotificationListener != null)
                        iNotificationListener.onNext(value);

                } else if (UdooBluService.ACTION_DATA_WRITE.equals(action)) {
                    if (mOnResultMap.containsKey(address)) {
                        OnBluOperationResult<Boolean> onResultListener = mOnResultMap.get(address);
                        if (onResultListener != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                onResultListener.onSuccess(true);
                            } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                                onResultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                            } else {
                                onResultListener.onError(new UdooBluException(UdooBluException.BLU_GENERIC_ERROR));
                            }
                            mOnResultMap.remove(address);
                        }
                    }
                } else if (UdooBluService.ACTION_DATA_READ.equals(action)) {
                    IReaderListener<byte[]> iReaderListener = mIReaderListenerMap.get(keySearch);
                    if (iReaderListener != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS)
                            iReaderListener.oRead(value);
                        else
                            iReaderListener.onError(new UdooBluException(UdooBluException.BLU_READ_CHARAC_ERROR));

                        mOnResultMap.remove(address);
                    }
                }
            } else if ((UdooBluService.ACTION_DESCRIPTION_WRITE.equals(action))) {
                if (mOnResultMap.containsKey(address)) {
                    OnBluOperationResult<Boolean> onResultListener = mOnResultMap.get(address);
                    if (onResultListener != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            onResultListener.onSuccess(true);
                        } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                            onResultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                        } else {
                            onResultListener.onError(new UdooBluException(UdooBluException.BLU_GENERIC_ERROR));
                        }

                        mOnResultMap.remove(address);
                    }
                }
            } else if (UdooBluService.ACTION_GATT_DISCONNECTED.equals(action)) {
                IBleDeviceListener iBleDeviceListener = mDeviceListenerMap.get(address);
                if (iBleDeviceListener != null)
                    iBleDeviceListener.onDeviceDisconnect(address);
            }
        }
    };

    public void saveBluItem(Context context, String address, String name) {
        SharedPreferences sharedPref = context.getSharedPreferences(BLU_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(address, name);
        editor.apply();
    }

    public void getBluItems(final Context context, final OnResult<Map<String, String>> onResult) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                Map<String, String> values;
                SharedPreferences sharedPref = context.getSharedPreferences(BLU_FILE, Context.MODE_PRIVATE);
                values = (Map<String, String>) sharedPref.getAll();
                if (onResult != null)
                    onResult.onSuccess(values);
            }
        });
    }

    public void getBluItem(final Context context, final String address, final OnResult<String> onResult) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPref = context.getSharedPreferences(BLU_FILE, Context.MODE_PRIVATE);
                if (onResult != null)
                    onResult.onSuccess(sharedPref.getString(address, ""));
            }
        });
    }

    public UdooBlu getUdooBlu(String address) {
        UdooBlu udooBlu = null;

        if(mUdooBluConnected.containsKey(address))
            udooBlu = mUdooBluConnected.get(address);

        return udooBlu;
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(UdooBluService.ACTION_GATT_CONNECTED);
        fi.addAction(UdooBluService.ACTION_GATT_DISCONNECTED);
        fi.addAction(UdooBluService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(UdooBluService.ACTION_DATA_NOTIFY);
        fi.addAction(UdooBluService.ACTION_DATA_WRITE);
        fi.addAction(UdooBluService.ACTION_DATA_READ);
        fi.addAction(UdooBluService.ACTION_DESCRIPTION_WRITE);
        return fi;
    }

//    private UUID getCharacteristic(UUID confUuid) {
//        UUID characteristic = null;
//
//        if (confUuid.equals(UDOOBLE.UUID_ACC_CONF)) {
//            characteristic = UDOOBLE.UUID_ACC_PERI;
//        } else if (confUuid.equals(UDOOBLE.UUID_MAG_CONF)) {
//            characteristic = UDOOBLE.UUID_MAG_PERI;
//        } else if (confUuid.equals(UDOOBLE.UUID_GYR_CONF)) {
//            characteristic = UDOOBLE.UUID_GYR_PERI;
//        } else if (confUuid.equals(UDOOBLE.UUID_TEM_CONF)) {
//            characteristic = UDOOBLE.UUID_TEM_PERI;
//        }
//        return characteristic;
//    }

    public boolean discoveryServices(String address) {
        if (isBluManagerReady) {
            return mUdooBluService.scanServices(address);
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return false;
    }

    public boolean requestConnectionPriority(String address, int connectionPriority) {
        return mUdooBluService.requestConnectionPriority(address, connectionPriority);
    }

    public void readFirmwareVersion(final String address, final IReaderListener<byte[]> readerListener) {
        addOperation(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (isBluManagerReady) {
                    UUID servUuid = TIUUID.UUID_DEVINFO_SERV;
                    UUID dataUuid = TIUUID.UUID_DEVINFO_FWREV;
                    BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                        mUdooBluService.readCharacteristic(address, charac);
                        mIReaderListenerMap.put(address + charac.getUuid().toString(), readerListener);
                    } else {
                        if (readerListener != null)
                            readerListener.onError(new UdooBluException(UdooBluException.BLU_GATT_SERVICE_NOT_FOUND));
                    }
                }
                return null;
            }
        });
    }

    public BluetoothGattService getService(String address, UUID servUuid) {
        return mUdooBluService.getService(address, servUuid);
    }

    public void readCharacteristic(String address, BluetoothGattCharacteristic characteristic, IReaderListener<byte[]> readerListener) {
        if (characteristic != null) {
            mUdooBluService.readCharacteristic(address, characteristic);
            mIReaderListenerMap.put(address + characteristic.getUuid().toString(), readerListener);
        }
    }

    public void writeCharacteristic(String address, BluetoothGattCharacteristic characteristic, byte[] value, OnBluOperationResult<Boolean> onResultListener) {
        if (characteristic != null) {

            if (onResultListener != null)
                mOnResultMap.put(address, onResultListener);

            mUdooBluService.writeCharacteristic(address, characteristic, value);
        }
    }

    public boolean writeCharacteristicNonBlock(String address, BluetoothGattCharacteristic characteristic, byte[] value) {
        boolean result = false;
        if (characteristic != null) {
            result = mUdooBluService.writeCharacteristicNonBlock(address, characteristic, value);
        }
        return result;
    }

    public void subscribeNotification(String address, BluetoothGattCharacteristic characteristic, INotificationListener<byte[]> notificationListener) {
        if (characteristic != null) {
            mINotificationListenerMap.put(address + characteristic.getUuid().toString(), notificationListener);
            mUdooBluService.setCharacteristicNotification(address, characteristic, true);
            Log.i(TAG, "setNotification: ");
        }
    }

    public void unSubscribeNotification(String address, BluetoothGattCharacteristic characteristic, OnBluOperationResult<Boolean> onResultListener) {
        if (characteristic != null) {
            String keySearch = address + characteristic.getUuid().toString();

            if (mINotificationListenerMap.containsKey(keySearch))
                mINotificationListenerMap.remove(keySearch);

            mOnResultMap.put(address, onResultListener);
            mUdooBluService.setCharacteristicNotification(address, characteristic, false);
        }
    }

    public void disconnect(String address) {
        mUdooBluService.disconnect(address);
    }

    public boolean bond(String address) {
        return false;
    }

}
