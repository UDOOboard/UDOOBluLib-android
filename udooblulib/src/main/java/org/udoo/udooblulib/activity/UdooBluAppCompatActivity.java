package org.udoo.udooblulib.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.udoo.udooblulib.R;
import org.udoo.udooblulib.UdooBlu;
import org.udoo.udooblulib.exceptions.UdooBluException;
import org.udoo.udooblulib.interfaces.IBleDeviceListener;
import org.udoo.udooblulib.interfaces.IBluManagerCallback;
import org.udoo.udooblulib.manager.UdooBluManager;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.scan.BluScanCallBack;

/**
 * Created by harlem88 on 28/03/17.
 */

public abstract class UdooBluAppCompatActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_ENABLE_LOCATION = 3;
    private boolean mBleSupported = true;
    private static BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBtAdapter = null;
    private final String TAG = "UdooBluAct";
    private UdooBluManager mUdooBluManager;
    private boolean mScan;
    public static final int HIGH = IOPin.DIGITAL_VALUE.HIGH.ordinal();
    public static final int LOW = IOPin.DIGITAL_VALUE.LOW.ordinal();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            mBleSupported = false;
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            } else {
                if(isLocationProviderEnabled((LocationManager) getSystemService(Context.LOCATION_SERVICE))) {
                    initBluManager();
                }else{
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(myIntent, REQUEST_ENABLE_LOCATION);
                }
            }
        } else if (savedInstanceState == null && mBleSupported) {
            initBluManager();
        } else {
            //Error TODO
        }
    }


    private void initBluManager(){
        UdooBluManager.GetUdooBluManager(this, new IBluManagerCallback() {
            @Override
            public void onBluManagerReady(UdooBluManager udooBluManager) {
                mUdooBluManager = udooBluManager;
                onUdooBluManagerReady();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(isLocationProviderEnabled((LocationManager) getSystemService(Context.LOCATION_SERVICE))) {
                                initBluManager();
                            }else{
                                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(myIntent, REQUEST_ENABLE_LOCATION);
                            }
                        }
                    });
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.bt_on, Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_on, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_ENABLE_LOCATION:
                if (isLocationProviderEnabled((LocationManager) getSystemService(Context.LOCATION_SERVICE))) {
                    initBluManager();
                }
                break;
            default:
                Log.e(TAG, "Unknown request code");
                break;
        }
    }

    public void startScan(){
        if (mScan) {
            mUdooBluManager.scanLeDevice(false, scanCallback);
        }else{
            mUdooBluManager.scanLeDevice(true, scanCallback);
            mScan = true;
        }
    }

    public void stopScan(){
        if (mScan) {
            mUdooBluManager.scanLeDevice(false, scanCallback);
        }
    }

    public void connect(String address){
        if(mUdooBluManager != null){
            mUdooBluManager.connect(address, new IBleDeviceListener() {
                @Override
                public void onDeviceConnected(UdooBlu udooBlu) {
                    onBluConnected(udooBlu);
                }

                @Override
                public void onDeviceDisconnect(String address) {
                    onBluDisconnected(address);
                }

                @Override
                public void onError(UdooBluException runtimeException) {
                    onBluError(runtimeException);
                }
            });
        }
    }

    public abstract void onUdooBluManagerReady();
    public abstract void onBluConnected(UdooBlu udooBlu);
    public abstract void onBluDisconnected(String address);
    public abstract void onBluDiscovered(int rssi, BluetoothDevice udooBluDevice);
    public abstract void onBluScanFinished();
    public abstract void onBluScanFailed();

    private BluScanCallBack scanCallback = new BluScanCallBack() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result != null) {
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    onBluDiscovered(result.getRssi(), device);
                }
            }
        }

        @Override
        public void onScanFinished() {
            mScan = false;
            onBluScanFinished();
        }

        @Override
        public void onError(UdooBluException runtimeException) {
            mScan = false;
            onBluError(runtimeException);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mScan = false;
            onError(new UdooBluException(errorCode));
            onBluScanFailed();
        }
    };

    public void onBluError(UdooBluException e) {
        if (e != null) {
            Log.i(TAG, "onBluError: " + e.getReason());
            String err;
            switch (e.getReason()) {
                case UdooBluException.BLU_SERVICE_NOT_READY:
                    err = "Service not ready";
                    break;
                case UdooBluException.BLUETOOTH_CANNOT_START:
                    err = "Bluetooth not start";
                    break;
                case UdooBluException.BLUETOOTH_LE_NOT_SUPPORTED:
                    err = "ble non supported";
                    break;
                case UdooBluException.BLUETOOTH_DISABLED:
                    err = "Bluetooth disabled";
                    break;
                case UdooBluException.BLUETOOTH_NOT_AVAILABLE:
                    err = "Bluetooth not available";
                    break;
                case UdooBluException.LOCATION_PERMISSION_MISSING:
                    err = "Location permission missing";
                    break;
                case UdooBluException.LOCATION_SERVICES_DISABLED:
                    err = "Location disabled";
                    break;
                case UdooBluException.BLU_SEQ_OBSERVER_ERROR:
                    err = "BluManager error";
                    break;
                case UdooBluException.BLU_READ_CHARAC_ERROR:
                    err = "BluManager read char error";
                    break;
                case UdooBluException.BLU_WRITE_CHARAC_ERROR:
                    err = "BluManager write char error";
                    break;
                case UdooBluException.BLU_GATT_SERVICE_NOT_FOUND:
                    err = "BluManager service gatt not found";
                    break;
                case UdooBluException.BLU_SENSOR_NOT_FOUND:
                    err = "BluManager blu sensor not found";
                    break;
                case UdooBluException.BLU_WRITE_DESCR_ERROR:
                    err = "BluManager write descr error";
                    break;
                case UdooBluException.BLU_NOTIFICATION_ERROR:
                    err = "BluManager notification error";
                    break;
                case UdooBluException.BLU_WRITE_PERIOD_NOTIFICATION_ERROR:
                    err = "BluManager write period notification error";
                    break;
                case UdooBluException.BLU_GENERIC_ERROR:
                default:
                    err = "Generic error";
            }
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "onBluError: " + UdooBluException.BLU_GENERIC_ERROR);
        }
    }

    public boolean isLocationProviderEnabled(LocationManager locationManager) {
        return (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                (Build.MODEL.equals("UDOONEO-MX6SX") || Build.MODEL.equals("UDOO-MX6DQ")));
    }
}
