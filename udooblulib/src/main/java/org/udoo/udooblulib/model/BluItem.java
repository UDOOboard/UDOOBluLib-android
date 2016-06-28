package org.udoo.udooblulib.model;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import org.udoo.udooblulib.utils.BindableBoolean;

/**
 * Created by harlem88 on 16/02/16.
 */

public class BluItem implements Parcelable {
    public BindableBoolean connected;
    public String name;
    public String address;
    public String rssi;

    public BluItem() {
        connected = new BindableBoolean();
    }

    public static BluItem Builder(BluetoothDevice device, String rssi) {
        BluItem bluItem = new BluItem();
        bluItem.name = device.getName();
        bluItem.address = device.getAddress();
        bluItem.rssi = rssi;
        bluItem.connected.set(false);
        return bluItem;
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

    private BluItem(Parcel in) {
        this.name = in.readString();
        address = in.readString();
        rssi = in.readString();
    }

    public static final Creator<BluItem> CREATOR = new Creator<BluItem>() {

        @Override
        public BluItem createFromParcel(Parcel source) {
            return new BluItem(source);
        }

        @Override
        public BluItem[] newArray(int size) {
            return new BluItem[size];
        }
    };
}

