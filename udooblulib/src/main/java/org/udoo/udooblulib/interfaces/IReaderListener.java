package org.udoo.udooblulib.interfaces;

import org.udoo.udooblulib.exceptions.UdooBluException;

/**
 * Created by harlem88 on 09/06/16.
 */

public interface IReaderListener<T> {
     void oRead(T value);
     void onError(UdooBluException runtimeException);
}
