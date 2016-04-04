package org.udoo.udooblulib.model;

import android.graphics.drawable.Drawable;

/**
 * Created by harlem88 on 17/02/16.
 */
public class XYZSensor {
    public String name;
    public String x;
    public String y;
    public String z;
    public Drawable imgResource;


    public static XYZSensor Builder(String name, Drawable imgResource){
        XYZSensor xyzSensor = new XYZSensor();
        xyzSensor.name = name;
        xyzSensor.x= "0.00";
        xyzSensor.y= "0.00";
        xyzSensor.z= "0.00";
        xyzSensor.imgResource = imgResource;
        return xyzSensor;
    }
}
