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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.udoo.udooblulib.BuildConfig;
import org.udoo.udooblulib.exceptions.UdooBluException;
import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.interfaces.OnBluOperationResult;
import org.udoo.udooblulib.interfaces.OnCharacteristicsListener;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.scan.BluScanCallBack;
import org.udoo.udooblulib.sensor.Constant;
import org.udoo.udooblulib.sensor.UDOOBLE;
import org.udoo.udooblulib.sensor.UDOOBLESensor;
import org.udoo.udooblulib.service.UdooBluService;
import org.udoo.udooblulib.utils.BitUtility;

import java.util.HashMap;
import java.util.Observer;
import java.util.UUID;

/**
 * Created by harlem88 on 24/03/16.
 */

public class UdooBluManagerImpl {
    private boolean mBound;
    private UdooBluService mUdooBluService;
    private HashMap<String, OnBluOperationResult<Boolean>> mOnResultMap;
    private HashMap<String, IBleDeviceListener> mDeviceListenerMap;
    private HashMap<String, OnCharacteristicsListener> mOnCharacteristicsListenerMap;
    private boolean isBluManagerReady;
    private Handler mHandler;
    private boolean mScanning;
    private String TAG = "BluManager";
    private IBluManagerCallback mIBluManagerCallback;


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
    public enum SENSORS {ACC ,MAGN, GYRO,TEMP,BAR,HUM, LIG}
    boolean[] sensorsReleved = new boolean[8];
    boolean[] sensorsEnabled = new boolean[8];

    public interface IBluManagerCallback {
        void onBluManagerReady();
    }

    public UdooBluManagerImpl(Context context) {
        init(context);
    }

    private void init(Context context) {

        mDeviceListenerMap = new HashMap<>();
        mOnCharacteristicsListenerMap = new HashMap<>();
        mOnResultMap = new HashMap<>();
        context.bindService(new Intent(context, UdooBluService.class), mConnection, Context.BIND_AUTO_CREATE);
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        context.registerReceiver(mGattBoundReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void setIBluManagerCallback(IBluManagerCallback iBluManagerCallback) {
        mIBluManagerCallback = iBluManagerCallback;
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
                    mIBluManagerCallback.onBluManagerReady();
            }
            else{
                isBluManagerReady = false;
            }

            if (BuildConfig.DEBUG)
                Log.i(TAG, "connected to udooBluService");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void scanLeDevice(boolean enable, BluScanCallBack scanCallback) {
        if (isBluManagerReady) {
            mUdooBluService.scanLeDevice(enable, scanCallback);
        } else if (scanCallback != null)
            scanCallback.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
    }

    public void connect(final String address, IBleDeviceListener iBleDeviceListener) {
        if (isBluManagerReady) {
            mUdooBluService.connect(address, iBleDeviceListener);
            mDeviceListenerMap.put(address, iBleDeviceListener);
        } else if (iBleDeviceListener != null)
            iBleDeviceListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
    }


    private void readSensors(String address, OnCharacteristicsListener onCharacteristicsListener) {
        if (isBluManagerReady) {
            UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
            UUID dataUuid = UDOOBLE.UUID_IOPIN_DIGITAL_DATA;

            BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);

                mUdooBluService.readCharacteristic(address, charac);
                mOnCharacteristicsListenerMap.put(address + charac.getUuid().toString(), onCharacteristicsListener);

            } else {
                Log.i(TAG, "error not service for this CharacteristicModel");
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
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

    private boolean enableSensor(String address, UDOOBLESensor sensor, boolean enable, Observer observer) {
        boolean success = false;
        if (isBluManagerReady) {
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

                    mUdooBluService.writeCharacteristic(address, charac, value);
                } catch (Exception e) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, "error enableSensor(), service uuid: " + servUuid.toString());
                }
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    public boolean enableNotification(String address, boolean enable, UDOOBLESensor sensor, OnCharacteristicsListener onCharacteristicsListener) {
        boolean success = false;
        if (isBluManagerReady) {

            UUID servUuid = sensor.getService();
            UUID dataUuid = sensor.getData();
            BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                success = mUdooBluService.setCharacteristicNotification(address, charac, enable);
                if (success) {
                    mOnCharacteristicsListenerMap.put(address + charac.getUuid().toString(), onCharacteristicsListener);
                    Log.i(TAG, "enableNotifications service " + servUuid.toString() + " is null: ");
                }
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    public boolean setNotificationPeriod(String address, UDOOBLESensor sensor) {
        return setNotificationPeriod(address, sensor, Constant.NOTIFICATIONS_PERIOD);
    }

    /* @param period is millisecond*/
    public boolean setNotificationPeriod(String address, UDOOBLESensor sensor, int period) {
        boolean success = false;
        if (isBluManagerReady) {
            if (sensor != null) {
                UUID servUuid = sensor.getService();
                UUID confUuid = sensor.getConfig();

                BluetoothGattService serv = mUdooBluService.getService(address, servUuid);
                BluetoothGattCharacteristic charac = null;
                if (serv != null) {
                    charac = serv.getCharacteristic(getCharacteristic(confUuid));
                    byte value = (byte) period;
                    byte[] msg = new byte[1];
                    msg[0] = value;
                    mUdooBluService.writeCharacteristic(address, charac, msg);
                    Log.i(TAG, "enable notification period: " + value);
                }
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    public void setIoPinMode(String address, final OnBluOperationResult<Boolean> onResultListener, IOPin... ioPins) {
        if (isBluManagerReady) {
            UUID service, characteristic;
            byte msg[] = new byte[2];
            service = UDOOBLE.UUID_IOPIN_SERV;
            characteristic = UDOOBLE.UUID_IOPIN_PIN_MODE;
            IOPin ioPin;
            int len = ioPins.length;
            if (len > 8)
                len = 8;

            for (int i = 0; i < len; i++) {
                ioPin = ioPins[i];
                byte mode = ioPin.getPinMode();
                int shift = 0;
                switch (ioPin.pin) {
                    case D7:
                    case A3:
                        shift = 4;
                        break;
                    case D6:
                    case A2:
                        shift = 3;
                        break;
                    case A5:
                    case A1:
                        shift = 2;
                        break;
                    case A4:
                    case A0:
                        shift = 1;
                        break;
                }
                byte value = (byte) ((mode << shift) | 0xff);
                int idx = ioPin.getPinValue() >= 16 ? 1 : 0;
                msg[idx] = (byte) (msg[idx] | value);
            }

            BluetoothGattService serv = mUdooBluService.getService(address, service);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                mOnResultMap.put(address, onResultListener);
                mUdooBluService.writeCharacteristic(address, charac, msg);
            if (BuildConfig.DEBUG)
                BitUtility.LogBinValue(msg, false);
            }
        }else if (onResultListener != null) {
            onResultListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

    public boolean digitalRead(final String address, OnCharacteristicsListener onCharacteristicsListener) {
        boolean success = false;
        if (isBluManagerReady) {
            UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
            UUID dataUuid = UDOOBLE.UUID_IOPIN_DIGITAL_DATA;

            BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);

                mUdooBluService.readCharacteristic(address, charac);
                mOnCharacteristicsListenerMap.put(address + charac.getUuid().toString(), onCharacteristicsListener);

            } else {
                Log.i(TAG, "error not service for this CharacteristicModel");
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    public boolean digitalWrite(String address, Observer observer, IOPin... ioPins) {
        boolean success = false;
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
                    case D7: shift = 8;break;
                    case D6: shift = 7;break;
                    case A5: shift = 6;break;
                    case A4: shift = 5;break;
                    case A3: shift = 4;break;
                    case A2: shift = 3;break;
                    case A1: shift = 2;break;
                    case A0: shift = 1;break;
                }
                byte value = (byte) ((ioPin.getDigitalValue() << shift) | 0xff);
                msg[0] = (byte) (msg[0] | value);
            }

            BluetoothGattService serv = mUdooBluService.getService(address, service);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                mUdooBluService.writeCharacteristic(address, charac, msg);
                success = true;

                if (BuildConfig.DEBUG)
                    BitUtility.LogBinValue(msg, false);
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    private boolean setPinAnalogOrPwmIndex(String address, IOPin ioPin) {
        boolean success = false;
        if (isBluManagerReady) {

            UUID service = null, characteristic = null;
            byte[] msg = null;

            service = UDOOBLE.UUID_IOPIN_SERV;
            characteristic = UDOOBLE.UUID_IOPIN_PWM_ANALOG_INDEX;

            msg = new byte[1];
            msg[0] = ioPin.getIndexValue();

            BluetoothGattService serv = mUdooBluService.getService(address, service);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                mUdooBluService.writeCharacteristic(address, charac, msg);


                success = true;
            }

            if (BuildConfig.DEBUG)
                BitUtility.LogBinValue(msg, false);

        }
        return success;
    }

    public void readAnalog(final String address, IOPin.IOPIN_PIN pin,final OnCharacteristicsListener onCharacteristicsListener) {
        if (isBluManagerReady) {
            setPinAnalogOrPwmIndex(address, IOPin.Builder(pin, IOPin.IOPIN_INDEX_VALUE.ANALOG));
            mOnResultMap.put(address, new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    if (aBoolean)
                        configAnalog(address, onCharacteristicsListener);
                    else if (onCharacteristicsListener != null)
                        onCharacteristicsListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));

                    mOnResultMap.remove(this);
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    if (onCharacteristicsListener != null)
                        onCharacteristicsListener.onError(runtimeException);
                    mOnResultMap.remove(this);
                }
            });
        } else if (BuildConfig.DEBUG) {
            if (onCharacteristicsListener != null)
                onCharacteristicsListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
            Log.i(TAG, "BluManager not ready");
        }
    }

    private void configAnalog(String address, OnCharacteristicsListener onCharacteristicsListener){
        UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
        UUID dataUuid = UDOOBLE.UUID_IOPIN_ANALOG_READ;

        BluetoothGattService serv = mUdooBluService.getService(address, servUuid);
        if (serv != null) {
            BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
            mUdooBluService.readCharacteristic(address, charac);
            mOnCharacteristicsListenerMap.put(address + charac.getUuid().toString(), onCharacteristicsListener);
        } else {

            if (onCharacteristicsListener != null)
                onCharacteristicsListener.onError(new UdooBluException(UdooBluException.BLU_GENERIC_ERROR));

            if (BuildConfig.DEBUG)
                Log.i(TAG, "error not service for this CharacteristicModel");
        }
    }

    public void setPwm(final String address, final IOPin.IOPIN_PIN pin, final int freq, final int dutyCycle, final OnBluOperationResult<Boolean> resultListener) {
        if (isBluManagerReady) {
            setPinAnalogOrPwmIndex(address, IOPin.Builder(pin, IOPin.IOPIN_INDEX_VALUE.PWM));
            mOnResultMap.put(address, new OnBluOperationResult<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    if (aBoolean){
                        configPwm(address, pin, freq, dutyCycle, resultListener);
                        mOnResultMap.put(address, resultListener);
                    } else
                        resultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));

                    mOnResultMap.remove(this);
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    if(resultListener != null)
                        resultListener.onError(runtimeException);

                    mOnResultMap.remove(this);
                }
            });
        } else if (resultListener != null) {
            resultListener.onError(new UdooBluException(UdooBluException.BLU_SERVICE_NOT_READY));
        }
    }

    private void configPwm(final String address, IOPin.IOPIN_PIN pin, final int freq, final int dutyCycle, OnBluOperationResult<Boolean> resultListener){
        UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
        UUID dataUuid = UDOOBLE.UUID_IOPIN_PWM_CONF;
        byte msg[] = new byte[5];
        if (freq >= 3 && freq <= 24000000) {
            byte freqs[] = BitUtility.ToBytes(freq);
            msg[0] = freqs[0];
            msg[1] = freqs[1];
            msg[2] = freqs[2];
            msg[3] = freqs[3];

            msg[4] = (byte) (dutyCycle | 0xff);

            BluetoothGattService serv = mUdooBluService.getService(address, servUuid);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
//                            mUdooBluService.writeCharacteristic(address, charac, msg, new Observer() {
//                                @Override
//                                public void update(Observable observable, Object data) {
//                                    if (data instanceof UdooBluException) {
//                                        if (resultListener != null)
//                                            resultListener.onError((UdooBluException) data);
//                                    } else if (resultListener != null) {
//                                        resultListener.onSuccess(true);
//                                    }
//                                }
//                            });
            } else {
                Log.i(TAG, "errate freq value");
            }
        } else {
            if (BuildConfig.DEBUG)
                Log.i(TAG, "error not service for this CharacteristicModel");

            if (resultListener != null) {
                resultListener.onError(new UdooBluException(UdooBluException.BLU_GENERIC_ERROR));
            }
        }
    }

    public boolean turnLed(String address, int color, byte func, int millis) {
        BluetoothGattService serv;
        BluetoothGattCharacteristic charac = null;
        boolean succendSend = false;
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
        } else {
            //TODO service not found
        }
        return succendSend;
    }

    private final BroadcastReceiver mGattBoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {

                // Retrieve the bond state and the device involved
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // If the device has been paired...
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    // ...check whether is supported and add it to the list
                    Log.i(TAG, "onReceive: bonded");
                }

                // If the device has been unpaired...
                else if (bondState == BluetoothDevice.BOND_NONE) {
                    // ...remove it from the list
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
                if(BuildConfig.DEBUG)
                    Log.i(TAG, "onReceive: connect");

                mUdooBluService.scanServices(address);
            } else if (UdooBluService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "onReceive: discover services");

                    if (mDeviceListenerMap.containsKey(address)) {
                        mUdooBluService.bond(address);

                        readSensors(address, new OnCharacteristicsListener() {
                            @Override
                            public void onCharacteristicsRead(String uuidStr, byte[] value, int status) {
                                for(int i = 0; i<value.length; i++){
                                    sensorsReleved[i] = (value[0] << i) == 1;
                                }

                                IBleDeviceListener iBleDeviceListener = mDeviceListenerMap.get(address);
                                if (iBleDeviceListener != null)
                                    iBleDeviceListener.onDeviceConnected();
                            }

                            @Override
                            public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {}

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
                if (mOnCharacteristicsListenerMap.containsKey(keySearch)) {
                    OnCharacteristicsListener onCharacteristicsListener = mOnCharacteristicsListenerMap.get(keySearch);

                    if (UdooBluService.ACTION_DATA_NOTIFY.equals(action)) {
                        // Notification
                        if (onCharacteristicsListener != null)
                            onCharacteristicsListener.onCharacteristicChanged(uuidStr, value);

                    } else if (UdooBluService.ACTION_DATA_WRITE.equals(action)) {
                        if(mOnResultMap.containsKey(address)){
                            OnBluOperationResult<Boolean> onResultListener = mOnResultMap.get(address);
                            if (onResultListener != null) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    onResultListener.onSuccess(true);
                                } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                                    onResultListener.onError(new UdooBluException(UdooBluException.BLU_WRITE_CHARAC_ERROR));
                                } else {
                                    onResultListener.onError(new UdooBluException(UdooBluException.BLU_GENERIC_ERROR));
                                }
                            }
                        }
                    } else if (UdooBluService.ACTION_DATA_READ.equals(action)) {
                        if (onCharacteristicsListener != null)
                            onCharacteristicsListener.onCharacteristicsRead(uuidStr, value, status);
                    }
                }
            }
        }

    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(UdooBluService.ACTION_GATT_CONNECTED);
        fi.addAction(UdooBluService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(UdooBluService.ACTION_DATA_NOTIFY);
        fi.addAction(UdooBluService.ACTION_DATA_WRITE);
        fi.addAction(UdooBluService.ACTION_DATA_READ);
        return fi;
    }

    private UUID getCharacteristic(UUID confUuid) {
        UUID characteristic = null;

        if (confUuid.equals(UDOOBLE.UUID_ACC_CONF)) {
            characteristic = UDOOBLE.UUID_ACC_PERI;
        } else if (confUuid.equals(UDOOBLE.UUID_MAG_CONF)) {
            characteristic = UDOOBLE.UUID_MAG_PERI;
        } else if (confUuid.equals(UDOOBLE.UUID_GYR_CONF)) {
            characteristic = UDOOBLE.UUID_GYR_PERI;
        } else if (confUuid.equals(UDOOBLE.UUID_TEM_CONF)) {
            characteristic = UDOOBLE.UUID_TEM_PERI;
        }
        return characteristic;
    }

    public boolean discoveryServices(String address) {
        if (isBluManagerReady) {
            return mUdooBluService.scanServices(address);
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return false;
    }

    public void disconnect(String address){
        mUdooBluService.disconnect(address);
    }
}
