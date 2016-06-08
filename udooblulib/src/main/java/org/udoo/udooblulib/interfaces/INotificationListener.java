package org.udoo.udooblulib.interfaces;

import org.udoo.udooblulib.exceptions.UdooBluException;

/**
 * Created by harlem88 on 08/06/16.
 */

public interface INotificationListener<T>{
    void onNext(T value);
    void onError(UdooBluException runtimeException);
}
