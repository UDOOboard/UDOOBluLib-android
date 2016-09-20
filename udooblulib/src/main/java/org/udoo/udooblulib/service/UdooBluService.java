package org.udoo.udooblulib.service;

/**
 * Created by harlem88 on 16/02/16.
 */

import android.Manifest;
import android.app.Service;
import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.udoo.udooblulib.BuildConfig;
import org.udoo.udooblulib.common.GattInfo;
import org.udoo.udooblulib.exceptions.UdooBluException;
import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.scan.BluScanCallBack;
import org.udoo.udooblulib.sensor.UDOOBLE;
import org.udoo.udooblulib.utils.SeqObserverQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for managing connection and data communication with a GATT server hosted on a given Bluetooth LE device.
 */

public class UdooBluService extends Service {
    static final String TAG = "BluetoothLeService";

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "ACTION_DATA_WRITE";
    public final static String ACTION_DESCRIPTION_WRITE = "ACTION_DESCRIPTION_WRITE";
    public final static String EXTRA_DATA = "EXTRA_DATA";
    public final static String EXTRA_UUID = "EXTRA_UUID";
    public final static String EXTRA_STATUS = "EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "EXTRA_ADDRESS";

    // BLE
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBtAdapter = null;
    private static final long SCAN_PERIOD = 60000;

     // Write/read pending response
    private HashMap<String, BluetoothGatt> mBluetoothGatts;
    private BlockingQueue<Callable> voidBlockingQueue = new LinkedBlockingQueue<>(10);
    private SeqObserverQueue seqObserverQueue = new SeqObserverQueue<>(voidBlockingQueue);
    private BluetoothLeScanner mLEScanner;
    private AtomicBoolean mScanning;

    private void broadcastUpdate(final String action, final String address, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String address, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
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

        return true;

    }

    private BluetoothGatt checkAndGetGattItem(String mac) {
        BluetoothGatt bluetoothGatt = null;

        if (mBluetoothGatts.containsKey(mac)) {
            bluetoothGatt = mBluetoothGatts.get(mac);
            if (bluetoothGatt != null) {
                if (mBtAdapter == null) {
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
            gattService = bluetoothGatt.getService(uuidLedServ);
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
        mScanning = new AtomicBoolean(false);
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


//    public String connectWithBounded() {
//        String address = "";
//        Set<BluetoothDevice> bluetoothDevices = mBtAdapter.getBondedDevices();
//        BluetoothDevice device = null;
//        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
//            if (bluetoothDevice.getAddress().startsWith("B0"))
//                device = bluetoothDevice;
//        }
//        if (device != null) {
//            if (connect(device.getAddress()))
//                address = device.getAddress();
//
//            Log.i(TAG, "connectWithBounded: try to connect... ");
//        }
//
//        return address;
//    }

    public boolean bond(final String address) {
        boolean success = false;
        Set<BluetoothDevice> bluetoothDevices = mBtAdapter.getBondedDevices();
        boolean bond = false;

        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            if (bluetoothDevice.getAddress().equals(address))
                bond = true;
        }
        if (!bond) {
            BluetoothGatt bluetoothGatt = checkAndGetGattItem(address);
            if (bluetoothGatt != null) {
                BluetoothDevice bluetoothDevice = bluetoothGatt.getDevice();
                if (bluetoothDevice != null)
                    success = bluetoothDevice.createBond();
            }
        } else success = true;
        return success;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
//    private boolean readCharacteristic(final String mac, final BluetoothGattCharacteristic characteristic) {
//        BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
//        boolean success = false;
//        if (bluetoothGatt != null && characteristic != null) {
//            success = bluetoothGatt.readCharacteristic(characteristic);
//        }
//        return success;
//    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final String mac, final BluetoothGattCharacteristic characteristic){
        try {
            voidBlockingQueue.put(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    BluetoothGatt bluetoothGatt = checkAndGetGattItem(mac);
                    boolean success = false;
                    if (bluetoothGatt != null && characteristic != null) {
                        success = bluetoothGatt.readCharacteristic(characteristic);
                    }
                    return success;
                }
            });

        } catch (InterruptedException e) {
            if(BuildConfig.DEBUG)
                Log.e(TAG, "readCharacteristic: " +e.getMessage());
        }
    }

    public void writeCharacteristic(final String address, final BluetoothGattCharacteristic characteristic, final byte[] b) {
        try {
            voidBlockingQueue.put(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    boolean result = false;
                    BluetoothGatt bluetoothGatt = checkAndGetGattItem(address);
                        if (bluetoothGatt != null) {
                            characteristic.setValue(b);
                            result = bluetoothGatt.writeCharacteristic(characteristic);
                            if (!result) {
                                broadcastUpdate(ACTION_DATA_WRITE, address, characteristic, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
                            }
                        }
                    return result;
                }
            });
        } catch (InterruptedException e) {
            broadcastUpdate(ACTION_DATA_WRITE, address, characteristic, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
            if (BuildConfig.DEBUG)
                Log.e(TAG, "writeCharacteristic: " + e.getMessage());
        }
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
    public void setCharacteristicNotification(final String address,final BluetoothGattCharacteristic characteristic,final boolean enable) {
        try {
            voidBlockingQueue.put(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    BluetoothGatt bluetoothGatt = checkAndGetGattItem(address);
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
                            result = bluetoothGatt.writeDescriptor(clientConfig);
                        }
                    } else {
                        Log.w(TAG, "setCharacteristicNotification failed");
                    }
                    return result;
                }
            });
        } catch (InterruptedException e) {
            broadcastUpdate(ACTION_DATA_WRITE, address, characteristic, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
            if (BuildConfig.DEBUG)
                Log.e(TAG, "writeCharacteristic: " + e.getMessage());
        }
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

    public void scanLeDevice(final boolean enable, final BluScanCallBack scanCallback) {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter scanFilter =new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UDOOBLE.UUID_SENSORS_SERV)).build();
        scanFilters.add(scanFilter);

        ScanSettings scanSettings = new ScanSettings.Builder().build();
        UdooBluException udooBluException = checkBluetooth(getApplicationContext());
        if (udooBluException != null) {
            if (scanCallback != null)
                scanCallback.onError(udooBluException);
        } else {
            mLEScanner = mBtAdapter.getBluetoothLeScanner();
            if (enable && mScanning.compareAndSet(false, true)){
                mLEScanner.startScan(scanFilters, scanSettings, scanCallback);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mLEScanner.stopScan(scanCallback);
                        mScanning.set(false);
                        if (scanCallback != null)
                            scanCallback.onScanFinished();
                    }
                }, SCAN_PERIOD);
            } else {
                mScanning.set(false);
                mLEScanner.stopScan(scanCallback);
                if (scanCallback != null)
                    scanCallback.onScanFinished();
            }
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public void connect(final String address, IBleDeviceListener iBleDeviceListener) {
        UdooBluException udooBluException = checkBluetooth(getApplicationContext());
        if (udooBluException != null) {
            if (iBleDeviceListener != null)
                iBleDeviceListener.onError(udooBluException);
        } else {
            final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
            int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

            BluetoothGatt bluetoothGatt = checkAndGetGattItem(address);
            if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
//                 Previously connected device. Try to reconnect.
                if (bluetoothGatt != null) {
                    Log.d(TAG, "Re-use GATT connection");
                    if (bluetoothGatt.connect()) {
                    } else {
                        Log.w(TAG, "GATT re-connect failed.");
                    }
                } else if (device == null) {
                    Log.w(TAG, "Device not found.  Unable to connect.");
                } else {
                    Log.d(TAG, "Create a new GATT connection.");
                    bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallbackBuilder());
                    mBluetoothGatts.put(address, bluetoothGatt);
                }
            } else {
                Log.w(TAG, "Attempt to connect in state: " + connectionState);
                bond(address);
                bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallbackBuilder());
                mBluetoothGatts.put(address, bluetoothGatt);
            }
        }
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

//    private boolean waitIdle(int i) {
//        i /= 10;
//        while (--i > 0) {
//            if (mBusy.get())
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            else
//                break;
//        }
//
//        return i > 0;
//    }

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
                Log.i(TAG, "onDescriptorRead");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                broadcastUpdate(ACTION_DESCRIPTION_WRITE, gatt.getDevice().getAddress(), status);
            }
        };

    }

    public UdooBluException checkBluetooth(final Context context){
        UdooBluException udooBluException = null;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            udooBluException = new UdooBluException(UdooBluException.BLUETOOTH_LE_NOT_SUPPORTED);
        } else {
            if (mBluetoothManager == null)
                mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager == null)
                udooBluException = new UdooBluException(UdooBluException.BLUETOOTH_CANNOT_START);

            if (mBtAdapter == null)
                mBtAdapter = mBluetoothManager.getAdapter();

            if (mBtAdapter == null || !mBtAdapter.isEnabled())
                udooBluException = new UdooBluException(UdooBluException.BLUETOOTH_DISABLED);

            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isLocationPermissionApproved(context))
                    udooBluException = new UdooBluException(UdooBluException.LOCATION_PERMISSION_MISSING);
                else if (!isLocationProviderEnabled((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)))
                    udooBluException = new UdooBluException(UdooBluException.LOCATION_SERVICES_DISABLED);
            }
        }
        return udooBluException;
    }

    public boolean isLocationPermissionApproved(Context context) {
        return isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, context)
                || isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, context);
    }

    public boolean isLocationProviderEnabled(LocationManager locationManager) {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_WATCH);
    }

    private boolean isPermissionGranted(String permission, Context context) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
