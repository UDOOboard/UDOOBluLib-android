package org.udoo.udooblulib.scan;

import android.bluetooth.le.ScanCallback;

import org.udoo.udooblulib.exceptions.UdooBluException;

/**
 * Created by harlem88 on 03/04/16.
 */
public abstract class BluScanCallBack extends ScanCallback {

    /**
     * Callback when scan is finished or stopped.
     */
    public abstract void onScanFinished();

    public abstract void onError(UdooBluException runtimeException);
}
