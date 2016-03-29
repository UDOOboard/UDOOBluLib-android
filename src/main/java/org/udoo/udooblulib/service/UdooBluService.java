package org.udoo.udooblulib.service;

/**
 * Created by harlem88 on 16/02/16.
 */

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.udoo.udooblulib.common.GattInfo;
import org.udoo.udooblulib.sensor.Constant;
import org.udoo.udooblulib.sensor.UDOOBLESensor;
import org.udoo.udooblulib.utils.SeqObserverQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Service for managing connection and data communication with a GATT server hosted on a given Bluetooth LE device.
 */

public class UdooBluService extends Service {
    static final String TAG = "BluetoothLeService";

    public final static String ACTION_GATT_CONNECTED = "com.aidilab.ble.common.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.aidilab.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.aidilab.ble.common.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "com.aidilab.ble.common.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "com.aidilab.ble.common.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "com.aidilab.ble.common.ACTION_DATA_WRITE";
    public final static String EXTRA_DATA = "com.aidilab.ble.common.EXTRA_DATA";
    public final static String EXTRA_UUID = "com.aidilab.ble.common.EXTRA_UUID";
    public final static String EXTRA_STATUS = "com.aidilab.ble.common.EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "com.aidilab.ble.common.EXTRA_ADDRESS";

    // BLE
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBtAdapter = null;

    private volatile boolean mBusy = false; // Write/read pending response
    private HashMap<String, BluetoothGatt> mBluetoothGatts;
    private BlockingQueue<Callable> voidBlockingQueue = new LinkedBlockingQueue<>(10);
    private SeqObserverQueue seqObserverQueue = new SeqObserverQueue<>(voidBlockingQueue);


    private void broadcastUpdate(final String action, final String address, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private void broadcastUpdate(final String action, final String address, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private boolean checkGatt(String mac) {
        if (mBtAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if (mBluetoothGatts.containsKey(mac) && mBluetoothGatts.get(mac) != null) {
            Log.w(TAG, "BluetoothGatt :" + mac + " not initialized");
            return false;
        }

        if (mBusy) {
            Log.w(TAG, "LeService busy");
            return false;
        }
        return true;

    }

    private BluetoothGatt checkAndGetGattItem(String mac) {
        BluetoothGatt bluetoothGatt = null;

        if (mBluetoothGatts.containsKey(mac)) {
            bluetoothGatt = mBluetoothGatts.get(mac);
            if (bluetoothGatt != null) {
                if (mBtAdapter != null) {
                    if (mBusy) {
                        Log.w(TAG, "LeService busy");
                        bluetoothGatt = null;
                    }
                } else {
                    bluetoothGatt = null;
                    Log.w(TAG, "BluetoothAdapter not initialized");
                }
            } else
                Log.w(TAG, "BluetoothGatt :" + mac + " not initialized");
        }
        return bluetoothGatt;
    }

    public BluetoothGattService getService(String mBleAddress, UUID uuidLedServ) {
        BluetoothGattService gattService = null;
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mBleAddress);
        if (bluetoothGatt != null) {
            mBusy = true;
            gattService = bluetoothGatt.getService(uuidLedServ);
            mBusy = false;
        }
        return gattService;
    }

    /**
     * Manage the BLE service
     */
    public class LocalBinder extends Binder {
        public UdooBluService getService() {
            return UdooBluService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: ");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: ");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnBind: ");
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {

        Log.d(TAG, "initialize");
        mBluetoothGatts = new HashMap<>();

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBtAdapter = mBluetoothManager.getAdapter();
        if (mBtAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        seqObserverQueue.run();
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        close();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG, "onRebind:");
    }

    //
    // GATT API
    //

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public boolean readCharacteristic(final String mac, final BluetoothGattCharacteristic characteristic) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean success = false;
        if (bluetoothGatt != null && characteristic != null) {
            mBusy = true;
            success = bluetoothGatt.readCharacteristic(characteristic);
        }
        return success;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final String mac, final BluetoothGattCharacteristic characteristic, final Observer observer) throws InterruptedException {
        seqObserverQueue.addObserver(observer);
        voidBlockingQueue.put(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
                boolean success = false;
                if (bluetoothGatt != null && characteristic != null) {
                    mBusy = true;
                    success = bluetoothGatt.readCharacteristic(characteristic);
                    waitIdle(Constant.GATT_TIMEOUT);
                }
                return success;
            }
        });

    }


    public boolean writeCharacteristic(String mac, BluetoothGattCharacteristic characteristic, byte b) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean result = false;
        if (bluetoothGatt != null && characteristic != null) {
            byte[] val = new byte[1];
            val[0] = b;
            characteristic.setValue(val);
            mBusy = true;
            result = bluetoothGatt.writeCharacteristic(characteristic);
        }
        return result;
    }

    public boolean writeCharacteristic(String mac, final BluetoothGattCharacteristic characteristic, byte[] b) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean result = false;
        if (bluetoothGatt != null) {
            characteristic.setValue(b);
            mBusy = true;
            result = bluetoothGatt.writeCharacteristic(characteristic);
        }
        return result;
    }

    public void writeCharacteristic(final String mac, final BluetoothGattCharacteristic characteristic,final byte[] b , Observer observer) throws InterruptedException {
        if(observer !=  null)
            seqObserverQueue.addObserver(observer);

        voidBlockingQueue.put(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
                boolean result = false;
                if (bluetoothGatt != null) {
                    characteristic.setValue(b);
                    mBusy = true;
                    result = bluetoothGatt.writeCharacteristic(characteristic);
                }
                return result;
            }
        });
    }

    public boolean enableSensor(final String mac, UDOOBLESensor sensor, boolean enable) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        BluetoothGattService serv;
        BluetoothGattCharacteristic charac = null;
        boolean success = false;
        if (bluetoothGatt != null) {

            serv = bluetoothGatt.getService(sensor.getService());
            if (serv != null) {
                charac = serv.getCharacteristic(sensor.getConfig());

                byte value = enable ? sensor.getEnableSensorCode()
                        : UDOOBLESensor.DISABLE_SENSOR_CODE;
                success = writeCharacteristic(mac, charac, value);
                waitIdle(Constant.GATT_TIMEOUT);
            }
        }
        return success;
    }

    public boolean enableNotification(String mac, UDOOBLESensor sensor, UUID characteristic,  boolean enable) {
        boolean success = false;
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        if(bluetoothGatt != null){
            UUID servUuid = sensor.getService();
            BluetoothGattService serv = bluetoothGatt.getService(servUuid);
            if(serv != null && characteristic != null){
                BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                success = bluetoothGatt.setCharacteristicNotification(charac, enable);
                Log.i(TAG, "enableNotifications service " + servUuid.toString() + " is null: ");
            }
        }
        return success;
    }

    public boolean writeNotificationPeriod(String mac, UDOOBLESensor sensor, UUID characteristic) {
        boolean success = false;
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        if(bluetoothGatt != null){
            BluetoothGattService serv = bluetoothGatt.getService(sensor.getService());
            if(serv != null){
                BluetoothGattCharacteristic charac = serv.getCharacteristic(characteristic);
                byte value = (byte) Constant.NOTIFICATIONS_PERIOD;
                charac.setValue(new byte[]{value});
                success = bluetoothGatt.writeCharacteristic(charac);
                waitIdle(Constant.GATT_TIMEOUT);
                Log.i(TAG, "enableNotifications service " + sensor.getService().toString() + " is null: ");
            }
        }
        return success;
    }


    public boolean writeCharacteristic(String mac, BluetoothGattCharacteristic characteristic, boolean b) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean result = false;
        if (bluetoothGatt != null) {
            byte[] val = new byte[1];

            val[0] = (byte) (b ? 1 : 0);
            characteristic.setValue(val);
            mBusy = true;
            result = bluetoothGatt.writeCharacteristic(characteristic);
        }
        return result;
    }

    public boolean writeCharacteristic(String mac, BluetoothGattCharacteristic characteristic) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean result = false;
        if (bluetoothGatt != null) {
            mBusy = true;
            result = bluetoothGatt.writeCharacteristic(characteristic);
        }
        return result;
    }

    public boolean scanServices(String mac) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean result = false;
        if (bluetoothGatt != null) {
            result = bluetoothGatt.discoverServices();
        }
        return result;
    }

    /**
     * Retrieves the number of GATT services on the connected device with @address.
     * This should be invoked only after {@code BluetoothGatt#discoverServices()} completes
     * successfully.
     *
     * @return A {@code integer} number of supported services.
     */
    public int getNumServices(String mac) {
        int res = 0;
        if (mBluetoothGatts.containsKey(mac)) {
            BluetoothGatt bluetoothGatt = mBluetoothGatts.get(mac);
            if (bluetoothGatt != null)
                res = bluetoothGatt.getServices().size();
        }
        return res;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device with @address.
     * This should be invoked only after {@code BluetoothGatt#discoverServices()} completes
     * successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String mac) {
        List<BluetoothGattService> list = new ArrayList<>();

        if (mBluetoothGatts.containsKey(mac)) {
            BluetoothGatt bluetoothGatt = mBluetoothGatts.get(mac);
            if (bluetoothGatt != null)
                list = bluetoothGatt.getServices();
        }

        return list;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enable         If true, enable notification. False otherwise.
     */
    public boolean setCharacteristicNotification(String mac, BluetoothGattCharacteristic characteristic, boolean enable) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean result = false;
        if (bluetoothGatt != null && bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig != null) {
                if (enable) {
                    Log.i(TAG, "enable notification");
                    clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    Log.i(TAG, "disable notification");
                    clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                mBusy = true;
                result = bluetoothGatt.writeDescriptor(clientConfig);
            }
        } else {
            Log.w(TAG, "setCharacteristicNotification failed");
        }
        return result;
    }

    public boolean isNotificationEnabled(String mac, BluetoothGattCharacteristic characteristic) {
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
        boolean result = false;
        if (bluetoothGatt != null) {
            BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig != null) {
                result = clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            }
        }
        return result;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public boolean connect(final String address) {
        if (mBtAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

            BluetoothGatt bluetoothGatt = checkAndGetGattItem(address);
            // Previously connected device. Try to reconnect.
            if (bluetoothGatt != null) {
                Log.d(TAG, "Re-use GATT connection");
                if (bluetoothGatt.connect()) {
                    return true;
                } else {
                    Log.w(TAG, "GATT re-connect failed.");
                    return false;
                }
            }

            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the
            // autoConnect parameter to false.
            Log.d(TAG, "Create a new GATT connection.");
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallbackBuilder());
            mBluetoothGatts.put(address, bluetoothGatt);
        } else {
            Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public void disconnect(String address) {
        if (mBtAdapter == null) {
            Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        BluetoothGatt bluetoothGatt = checkAndGetGattItem(address);
        if (bluetoothGatt != null) {
            Log.i(TAG, "disconnect");
            if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothGatt.disconnect();
            } else {
                Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
            }
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are released properly.
     */
    public void close() {
        Set<String> macs = mBluetoothGatts.keySet();
        for (String deviceMac : macs) {
            BluetoothGatt bluetoothGatt = mBluetoothGatts.get(deviceMac);
            if (bluetoothGatt != null) {
                bluetoothGatt.close();
                mBluetoothGatts.remove(deviceMac);
            }
        }
    }

    public int numConnectedDevices() {
        List<BluetoothDevice> devList;
        devList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        return devList.size();
    }

    public boolean waitIdle(int i) {
        i /= 10;
        while (--i > 0) {
            if (mBusy)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            else
                break;
        }

        return i > 0;
    }

    private BluetoothGattCallback bluetoothGattCallbackBuilder() {
        return new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (gatt == null) {
                    Log.e(TAG, "mBluetoothGatt not created!");
                    return;
                }

                BluetoothDevice device = gatt.getDevice();
                String address = device.getAddress();
                Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);

                try {
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
                            break;
                        case BluetoothProfile.STATE_DISCONNECTED:
                            broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
                            break;
                        default:
                            Log.e(TAG, "New state not processed: " + newState);
                            break;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothDevice device = gatt.getDevice();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(), status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                broadcastUpdate(ACTION_DATA_NOTIFY, gatt.getDevice().getAddress(), characteristic, BluetoothGatt.GATT_SUCCESS);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,  BluetoothGattCharacteristic characteristic, int status) {
                broadcastUpdate(ACTION_DATA_READ, gatt.getDevice().getAddress(), characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                broadcastUpdate(ACTION_DATA_WRITE, gatt.getDevice().getAddress(), characteristic, status);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                mBusy = false;
                Log.i(TAG, "onDescriptorRead");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                mBusy = false;
                Log.i(TAG, "onDescriptorWrite");
            }
        };

    }
}
