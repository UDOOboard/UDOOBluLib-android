package org.udoo.udooblutest;

import android.bluetooth.BluetoothDevice;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;

import org.udoo.udooblulib.UdooBlu;
import org.udoo.udooblulib.activity.UdooBluAppCompatActivity;
import org.udoo.udooblulib.exceptions.UdooBluException;
import org.udoo.udooblulib.interfaces.INotificationListener;
import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.sensor.UDOOBLE;
import org.udoo.udooblulib.sensor.UDOOBLESensor;
import org.udoo.udooblulib.utils.Point3D;
import org.udoo.udooblutest.databinding.ActivityMainBinding;

public class MainActivity extends UdooBluAppCompatActivity {
    private boolean isConnected;
    public ActivityMainBinding mViewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    }

    @Override
    public void onUdooBluManagerReady() {
        startScan();
    }

    @Override
    public void onBluConnected(final UdooBlu udooBlu) {
        if (udooBlu != null) {
            final IOPin d1 = IOPin.Builder(IOPin.PIN.A1, IOPin.MODE.DIGITAL_INPUT);
//            udooBlu.setIoPinMode(null, IOPin.Builder(IOPin.PIN.A0, IOPin.MODE.DIGITAL_OUTPUT), d1,
//                    IOPin.Builder(IOPin.PIN.A2, IOPin.MODE.ANALOG_INPUT));

            udooBlu.setIoPinMode(6, IOPin.MODE.DIGITAL_OUTPUT);

            udooBlu.subscribeNotificationTemperature(new INotificationListener<byte[]>() {
                @Override
                public void onNext(byte[] value) {
                    float temp = UDOOBLESensor.TEMPERATURE.convertTemp(value);
                    mViewBinding.tempValue.setText(temp + " °");
                }

                @Override
                public void onError(UdooBluException runtimeException) {

                }
            }, 2000);

            udooBlu.subscribeNotificationAccelerometer(new INotificationListener<byte[]>() {
                @Override
                public void onNext(byte[] value) {
                    Point3D point3D = UDOOBLESensor.ACCELEROMETER.convert(value);
                    Log.i("  sss", "onNext: point3D.x acc" +point3D.x);
                }

                @Override
                public void onError(UdooBluException runtimeException) {

                }
            }, 2000);

            udooBlu.subscribeNotificationGyroscope(new INotificationListener<byte[]>() {
                @Override
                public void onNext(byte[] value) {
                    Point3D point3D = UDOOBLESensor.GYROSCOPE.convert(value);
                    Log.i("  sss", "onNext: point3D.x agy" +point3D.x);
                }

                @Override
                public void onError(UdooBluException runtimeException) {

                }
            }, 2000);

            udooBlu.subscribeNotificationMagnetometer(new INotificationListener<byte[]>() {
                @Override
                public void onNext(byte[] value) {
                    Point3D point3D = UDOOBLESensor.MAGNETOMETER.convert(value);
                    Log.i("  sss", "onNext: point3D.x magn " +point3D.x);
                }

                @Override
                public void onError(UdooBluException runtimeException) {

                }
            }, 2000);

            mViewBinding.digital0.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    udooBlu.digitalWrite(6, isChecked ? HIGH : LOW);
                }
            });

            udooBlu.subscribeNotificationAnalog(IOPin.PIN.A2, new INotificationListener<byte[]>() {
                @Override
                public void onNext(byte[] value) {
                    short adc = UDOOBLESensor.IOPIN_ANALOG.convertADC(value);
                    mViewBinding.analogValue.setText(adc + " V");
                }

                @Override
                public void onError(UdooBluException runtimeException) {

                }
            });

            udooBlu.subscribeNotificationDigital(new INotificationListener<byte[]>() {
                @Override
                public void onNext(byte[] value) {
                    boolean[] pins= UDOOBLESensor.IOPIN_DIGITAL.convertIOPinDigital(value, d1);
                    mViewBinding.digitalIntValue.setText((pins[0] ? 1:0) + "");
                }

                @Override
                public void onError(UdooBluException runtimeException) {

                }
            });
        }
    }

    @Override
    public void onBluDisconnected(String address) {

    }

    @Override
    public void onBluDiscovered(int rssi, BluetoothDevice udooBluDevice) {
        if (!isConnected) {
            connect(udooBluDevice.getAddress());
            stopScan();
            isConnected = true;
        }
    }

    @Override
    public void onBluScanFinished() {

    }

    @Override
    public void onBluScanFailed() {

    }
}
