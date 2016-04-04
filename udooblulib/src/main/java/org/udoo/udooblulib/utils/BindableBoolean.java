package org.udoo.udooblulib.utils;

import android.databinding.BaseObservable;

/**
 * Created by harlem88 on 09/02/16.
 */

public class BindableBoolean extends BaseObservable {
    boolean mValue;

    public boolean get() {
        return mValue;
    }

    public void set(boolean value) {
        if (mValue != value) {
            this.mValue = value;
            notifyChange();
        }
    }
}
