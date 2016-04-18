package org.udoo.udooblulib.interfaces;


/**
 * Created by harlem88 on 17/02/16.
 */
public interface IBleDeviceListener {
    void onDeviceConnected();
    void onServicesDiscoveryCompleted(String address);
    void onDeviceDisconnect();
}
