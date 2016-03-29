package org.udoo.udooblulib.model;

import org.udoo.udooblulib.utils.BindableString;

/**
 * Created by harlem88 on 17/02/16.
 */
public class Temperature {
    public BindableString value;

    public Temperature() {
        value = new BindableString();
        value.set("");
    }

    public void setValue(String value) {
        this.value.set(value);
    }
}
