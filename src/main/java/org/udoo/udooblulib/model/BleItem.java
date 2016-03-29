package org.udoo.udooblulib.model;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import org.udoo.udooblulib.utils.BindableBoolean;

/**
 * Created by harlem88 on 16/02/16.
 */

public class BleItem implements Parcelable {
    public BindableBoolean connected;
    public String name;
    public String address;
    public String rssi;

    public BleItem() {
        connected = new BindableBoolean();
    }

    public static BleItem Builder(BluetoothDevice device, String rssi) {
        BleItem bleItem = new BleItem();
        bleItem.name = device.getName();
        bleItem.address = device.getAddress();
        bleItem.rssi = rssi;
        bleItem.connected.set(false);
        return bleItem;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Storing the Student data to Parcel object
     **/
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(rssi);
    }

    private BleItem(Parcel in) {
        this.name = in.readString();
        address = in.readString();
        rssi = in.readString();
    }

    public static final Creator<BleItem> CREATOR = new Creator<BleItem>() {

        @Override
        public BleItem createFromParcel(Parcel source) {
            return new BleItem(source);
        }

        @Override
        public BleItem[] newArray(int size) {
            return new BleItem[size];
        }
    };
}

