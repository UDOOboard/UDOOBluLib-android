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
import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.interfaces.OnCharacteristicsListener;
import org.udoo.udooblulib.scan.BluScanCallBack;
import org.udoo.udooblulib.sensor.Constant;
import org.udoo.udooblulib.sensor.Constant.IOPIN;
import org.udoo.udooblulib.sensor.Constant.IOPIN_MODE;
import org.udoo.udooblulib.sensor.Constant.IOPIN_TYPE;
import org.udoo.udooblulib.sensor.Constant.IOPIN_VALUE;
import org.udoo.udooblulib.sensor.UDOOBLE;
import org.udoo.udooblulib.sensor.UDOOBLESensor;
import org.udoo.udooblulib.service.UdooBluService;
import org.udoo.udooblulib.utils.BitUtility;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

/**
 * Created by harlem88 on 24/03/16.
 */

public class UdooBluManager {
    private boolean mBound;
    private UdooBluService mUdooBluService;
    private HashMap<String, IBleDeviceListener> mDeviceListenerMap;
    private HashMap<String, OnCharacteristicsListener> mOnCharacteristicsListenerMap;
    private boolean isBluManagerReady;
    private Handler mHandler;
    private boolean mScanning;
    private String TAG = "BluManager";
    private IBluManagerCallback mIBluManagerCallback;

    public interface IBluManagerCallback{
        void onBluManagerReady();
    }

    public UdooBluManager(Context context) {
        init(context);
    }

    private void init(Context context) {
        mDeviceListenerMap = new HashMap<>();
        mOnCharacteristicsListenerMap = new HashMap<>();
        context.bindService(new Intent(context, UdooBluService.class), mConnection, Context.BIND_AUTO_CREATE);
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        context.registerReceiver(mGattBoundReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mHandler = new Handler(Looper.getMainLooper());

    }

    public void setIBluManagerCallback(IBluManagerCallback iBluManagerCallback){
        mIBluManagerCallback = iBluManagerCallback;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            UdooBluService.LocalBinder binder = (UdooBluService.LocalBinder) service;
            mUdooBluService = binder.getService();
            mBound = true;
            if(mUdooBluService.initialize()){
                isBluManagerReady = true;
                if(mIBluManagerCallback != null)
                    mIBluManagerCallback.onBluManagerReady();
            }{
                isBluManagerReady = false;
            }

            if(BuildConfig.DEBUG)
                Log.i(TAG, "connected to udooBluService");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public boolean scanLeDevice(boolean enable, BluScanCallBack scanCallback) {
        boolean success = false;
        if (isBluManagerReady) {
            success = mUdooBluService.scanLeDevice(enable, scanCallback);
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");

        return success;
    }

    public void connect(String address, IBleDeviceListener iBleDeviceListener) {
        if (isBluManagerReady) {
            mDeviceListenerMap.put(address, iBleDeviceListener);
            mUdooBluService.connect(address);
        }else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");

    }

    public String connects(IBleDeviceListener iBleDeviceListener) {
        String address = "";
        if (isBluManagerReady) {
            address = mUdooBluService.connectWithBounded();
            if (address != null && address.length() > 0)
                mDeviceListenerMap.put(address, iBleDeviceListener);
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return address;
    }

    public boolean enableSensor(String address, UDOOBLESensor sensor, boolean enable) {
        boolean success = false;
        if (isBluManagerReady) {
            if (sensor != null) {
                UUID servUuid = sensor.getService();
                UUID confUuid = sensor.getConfig();
                BluetoothGattService serv = null;
                BluetoothGattCharacteristic charac = null;

                byte value;
                try {
                    serv = mUdooBluService.getService(address, servUuid);
                    charac = serv.getCharacteristic(confUuid);
                    value = enable ? sensor.getEnableSensorCode()
                            : UDOOBLESensor.DISABLE_SENSOR_CODE;
                    success = mUdooBluService.writeCharacteristic(address, charac, value);
                    mUdooBluService.waitIdle(Constant.GATT_TIMEOUT);
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
                mUdooBluService.waitIdle(Constant.GATT_TIMEOUT);
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
                    success = mUdooBluService.writeCharacteristic(address, charac, value);
                    Log.i(TAG, "enable notification period: " + value);
                    mUdooBluService.waitIdle(Constant.GATT_TIMEOUT);
                }
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    public boolean setIoPinMode(String address, IOPIN iopin, IOPIN_TYPE iopin_type, IOPIN_MODE iopin_mode) {
        boolean success = false;
        if (isBluManagerReady) {
            if (iopin != null && iopin_type != null && iopin_mode != null) {

                UUID service = null, characteristic = null;
                byte[] msg = null;

                if (iopin_type == IOPIN_TYPE.ANALOG && iopin_mode == IOPIN_MODE.INPUT && iopin != IOPIN.D6 && iopin != IOPIN.D7) {
                    service = UDOOBLE.UUID_IOPIN_SERV;
                    characteristic = UDOOBLE.UUID_IOPIN_ANALOG_CONF;
                    byte bPos = getByteMorePos(true, iopin);
                    msg = new byte[2];
                    msg[0] = 0;
                    msg[1] = bPos;

                } else if (iopin_type == IOPIN_TYPE.DIGITAL) {
                    service = UDOOBLE.UUID_IOPIN_SERV;
                    characteristic = UDOOBLE.UUID_IOPIN_DIGITAL_CONF;
                    msg = new byte[1];
                    msg[0] = getByteMorePos(iopin_mode == IOPIN_MODE.INPUT, iopin);
                }

                if (msg != null) {
                    BluetoothGattService serv = mUdooBluService.getService(address, service);
                    if (serv != null) {
                        BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                        mUdooBluService.writeCharacteristic(address, charac, msg);
                        mUdooBluService.waitIdle(Constant.GATT_TIMEOUT);
                        success = true;
                    }

                    if (BuildConfig.DEBUG)
                        BitUtility.LogBinValue(msg, false);
                }
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }


    public boolean digitalRead(final String address, OnCharacteristicsListener onCharacteristicsListener) {
        boolean success = false;
        if (isBluManagerReady) {
            UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
            UUID dataUuid = UDOOBLE.UUID_IOPIN_DIGITAL_DATA;

            BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                success = mUdooBluService.readCharacteristic(address, charac);
                if (success) {
                    mOnCharacteristicsListenerMap.put(address + charac.getUuid().toString(), onCharacteristicsListener);
                } else
                    Log.i(TAG, "error on set property for this CharacteristicModel");
            } else {
                Log.i(TAG, "error not service for this CharacteristicModel");
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    public boolean digitalWrite(String address, IOPIN_VALUE iopin_value, IOPIN iopin) {
        return digitalWrite(address, getBytePos(iopin_value == IOPIN_VALUE.HIGH, iopin));
    }

    private boolean digitalWrite(String address, byte value) {
        boolean success = false;
        if (isBluManagerReady) {
            byte[] msg;
            UUID service = UDOOBLE.UUID_IOPIN_SERV, characteristic;
            characteristic = UDOOBLE.UUID_IOPIN_DIGITAL_DATA;
            msg = new byte[1];
            msg[0] = value;
            BluetoothGattService serv = mUdooBluService.getService(address, service);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                try {
                    mUdooBluService.writeCharacteristic(address, charac, msg, new Observer() {
                        @Override
                        public void update(Observable observable, Object data) {
                            //TODO
                        }
                    });
                    success = true;
                } catch (InterruptedException e) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, "digitalWrite: " + e.getMessage());
                }
                BitUtility.LogBinValue(msg, false);
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
    }

    public boolean digitalWrite(String address, IOPIN_VALUE iopin_value, IOPIN... iopins) {
        byte bPos = getByteMorePos(iopin_value == IOPIN_VALUE.HIGH, iopins);
        return digitalWrite(address, bPos);
    }

    public boolean readAnalog(final String address, OnCharacteristicsListener onCharacteristicsListener) {
        boolean success = false;
        if (isBluManagerReady) {
            UUID servUuid = UDOOBLE.UUID_IOPIN_SERV;
            UUID dataUuid = UDOOBLE.UUID_IOPIN_ANALOG_READ;

            BluetoothGattService serv = mUdooBluService.getService(address, servUuid);

            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                success = mUdooBluService.readCharacteristic(address, charac);
                if (success) {
                    mOnCharacteristicsListenerMap.put(address + charac.getUuid().toString(), onCharacteristicsListener);
                } else
                    Log.i(TAG, "error on set property for this CharacteristicModel");
            } else {
                Log.i(TAG, "error not service for this CharacteristicModel");
            }
        } else if (BuildConfig.DEBUG)
            Log.i(TAG, "BluManager not ready");
        return success;
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
            succendSend = mUdooBluService.writeCharacteristic(address, charac, msg);
            mUdooBluService.waitIdle(Constant.GATT_TIMEOUT);
        } else {
            //TODO service not found
        }
        return succendSend;
    }

    private final BroadcastReceiver mGattBoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {

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
            String address = intent.getStringExtra(UdooBluService.EXTRA_ADDRESS);

            if (UdooBluService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "onReceive: connect" );
                mUdooBluService.scanServices(address);

//                if (mDeviceListenerMap.containsKey(address)) {
//                    IBleDeviceListener iBleDeviceListener = mDeviceListenerMap.get(address);
//                    if (iBleDeviceListener != null)
//                        iBleDeviceListener.onDeviceConnected();
//                }
            } else if (UdooBluService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "onReceive: discover services" );
                    if (mDeviceListenerMap.containsKey(address)) {
                        mUdooBluService.bond(address);

                        IBleDeviceListener iBleDeviceListener = mDeviceListenerMap.get(address);
                        if (iBleDeviceListener != null)
                            iBleDeviceListener.onServicesDiscoveryCompleted(address);
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
                        // Data written

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

    private byte getBytePos(boolean high, IOPIN iopin) {
        return BitUtility.setOnlyValuePosByte(high, getPinIntPos(iopin));
    }

    private byte getByteMorePos(boolean high, IOPIN... iopins){
        int values [] = new int[iopins.length];
        for (int i = 0; i<iopins.length; i++){
            values[i] = getPinIntPos(iopins[i]);
        }
        return BitUtility.setValuesPosByte(high, values);
    }

    private int getPinIntPos(IOPIN iopin) {
        int value = -1;

        switch (iopin) {
            case A0:
                value = 1;
                break;
            case A1:
                value = 2;
                break;
            case A2:
                value = 4;
                break;
            case A3:
                value = 8;
                break;
            case A4:
                value = 16;
                break;
            case A5:
                value = 32;
                break;
            case D6:
                value = 64;
                break;
            case D7:
                value = 128;
                break;
        }

        return value;
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
}
