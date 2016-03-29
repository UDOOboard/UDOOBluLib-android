package org.udoo.udooblulib.utils;

import android.databinding.BaseObservable;

/**
 * Created by harlem88 on 09/02/16.
 */


public class BindableInt extends BaseObservable {
    int mValue;

    public int get() {
        return mValue;
    }

    public void set(int value) {
        if (mValue != value) {
            this.mValue = value;
            notifyChange();
        }
    }
}
