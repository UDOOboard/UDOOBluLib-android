package org.udoo.udooblulib.interfaces;


import org.udoo.udooblulib.UdooBlu;
import org.udoo.udooblulib.exceptions.UdooBluException;

/**
 * Created by harlem88 on 17/02/16.
 */
public interface IBleDeviceListener {
    void onDeviceConnected(UdooBlu udooBlu);
    void onDeviceDisconnect(String address);
    void onError(UdooBluException runtimeException);
}
