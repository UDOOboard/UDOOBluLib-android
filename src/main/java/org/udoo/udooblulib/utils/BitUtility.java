package org.udoo.udooblulib.utils;

import android.util.Log;

/**
 * Created by harlem88 on 27/02/16.
 */
public class BitUtility {
    private static final String TAG = "BinUtility";

    public static byte setOnlyValuePosByte(boolean high, int pos) {
        if (pos > 8) return 0;
        return (byte) (((high ? 1 : 0) << pos) & 0xff);
    }

    public static byte setValuePosByte(boolean high, int pos, byte oldValue) {
        if (pos > 8) return 0;
        return (byte) ((((high ? 1 : 0) << pos) & 0xff) | oldValue);
    }

    public static void LogBinValue(byte[] value, boolean inverse) {
        if (value != null) {
            int size = value.length;
            if (inverse) {
                String sValueBin = "";
                for (int i = 0; i < size; i++) {
                    sValueBin = " " + value[i] + sValueBin;
                    for (int k = 0; k < 8; k++) {
                        sValueBin = (((value[i] & (1 << k)) != 0) ? "1" : "0") + sValueBin;
                    }
                    sValueBin = " " + sValueBin;
                }
                Log.i(TAG, "LogValue: " + sValueBin);
            } else {
                String sValueBin = "";
                for (int i = size - 1; i >= 0; i--) {
                    sValueBin = " " + value[i] + sValueBin;
                    for (int k = 0; k < 8; k++) {
                        sValueBin = (((value[i] & (1 << k)) != 0) ? "1" : "0") + sValueBin;
                    }
                    sValueBin = " " + sValueBin;
                }
                Log.i(TAG, "LogValue: " + sValueBin);
            }
        }else
            Log.i(TAG, "LogValue: null value");
    }
}
