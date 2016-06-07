package org.udoo.udooblulib.interfaces;

import org.udoo.udooblulib.exceptions.UdooBluException;

/**
 * Created by harlem88 on 10/02/16.
 */

public interface OnCharacteristicsListener {
    void onCharacteristicsRead(String uuidStr, byte[] value, int status);
    void onCharacteristicChanged(String uuidStr, byte[] rawValue);
    void onError(UdooBluException e);
}
