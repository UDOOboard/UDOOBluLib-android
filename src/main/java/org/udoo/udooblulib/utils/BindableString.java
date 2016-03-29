package org.udoo.udooblulib.utils;

import android.databinding.BaseObservable;

/**
 * Created by harlem88 on 19/02/16.
 */
public class BindableString extends BaseObservable {
    String mValue;

    public String get() {
        return mValue;
    }

    public void set(String value) {
        if (mValue != value) {
            this.mValue = value;
            notifyChange();
        }
    }
}
