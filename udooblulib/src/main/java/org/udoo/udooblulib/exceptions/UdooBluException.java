package org.udoo.udooblulib.exceptions;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by harlem88 on 31/05/16.
 */

public class UdooBluException extends RuntimeException {

    @IntDef({BLU_GENERIC_ERROR, BLU_SERVICE_NOT_READY, BLUETOOTH_LE_NOT_SUPPORTED, BLUETOOTH_CANNOT_START, BLUETOOTH_DISABLED, BLUETOOTH_NOT_AVAILABLE, LOCATION_PERMISSION_MISSING, LOCATION_SERVICES_DISABLED, BLU_SEQ_OBSERVER_ERROR, BLU_WRITE_CHARAC_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Reason {}

    public static final int BLU_GENERIC_ERROR = -1;
    public static final int BLU_SERVICE_NOT_READY = 0;
    public static final int BLUETOOTH_CANNOT_START = 1;
    public static final int BLUETOOTH_LE_NOT_SUPPORTED = 2;
    public static final int BLUETOOTH_DISABLED = 3;
    public static final int BLUETOOTH_NOT_AVAILABLE = 4;
    public static final int LOCATION_PERMISSION_MISSING = 5;
    public static final int LOCATION_SERVICES_DISABLED = 6;
    public static final int BLU_SEQ_OBSERVER_ERROR = 7;
    public static final int BLU_WRITE_CHARAC_ERROR = 8;

    private final int reason;

    public UdooBluException(int cause) {
        this.reason = cause;
    }

    /**
     * Returns the reason code of scan failure.
     *
     * @return One of {@link #BLUETOOTH_CANNOT_START}, {@link #BLUETOOTH_DISABLED}, {@link #BLUETOOTH_NOT_AVAILABLE},
     * {@link #LOCATION_PERMISSION_MISSING}, {@link #LOCATION_SERVICES_DISABLED}.
     */
    @Reason
    public int getReason() {
        return reason;
    }
}
