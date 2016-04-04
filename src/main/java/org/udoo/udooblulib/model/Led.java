package org.udoo.udooblulib.model;

import android.graphics.Color;

import org.udoo.udooblulib.utils.BindableBoolean;
import org.udoo.udooblulib.utils.BindableInt;

/**
 * Created by harlem88 on 09/02/16.
 */

public class Led{

    public BindableInt color;
    public BindableBoolean onoff;
    public BindableBoolean blink;

    public Led(){
        onoff = new BindableBoolean();
        blink = new BindableBoolean();
        color = new BindableInt();
    }

    public static Led BuilderDefault(){
        Led led = new Led();
        led.color.set(Color.WHITE);
        led.blink.set(false);
        led.onoff.set(false);
        return led;
    }

    public static Led Builder(byte status, int color){
        Led led = new Led();

        if(status == 2){
            led.blink.set(true);
            led.onoff.set(true);
        }
        else if (status == 1){
            led.blink.set(false);
            led.onoff.set(true);
        }else {
            led.blink.set(false);
            led.onoff.set(true);
        }
        led.color.set(color);
        return led;
    }
}
