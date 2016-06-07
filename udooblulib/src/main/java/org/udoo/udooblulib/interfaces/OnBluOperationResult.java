package org.udoo.udooblulib.interfaces;

import org.udoo.udooblulib.exceptions.UdooBluException;

/**
 * Created by harlem88 on 06/06/16.
 */

public interface OnBluOperationResult<T>{
    void onSuccess(T t);
    void onError(UdooBluException runtimeException);
}
