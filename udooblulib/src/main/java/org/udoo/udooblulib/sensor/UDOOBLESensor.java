/**************************************************************************************************
  Filename:       Sensor.java
  Revised:        $Date: 2013-08-30 11:44:31 +0200 (fr, 30 aug 2013) $
  Revision:       $Revision: 27454 $

  Copyright 2013 Texas Instruments Incorporated. All rights reserved.
 
  IMPORTANT: Your use of this Software is limited to those specific rights
  granted under the terms of a software license agreement between the user
  who downloaded the software, his/her employer (which must be your employer)
  and Texas Instruments Incorporated (the "License").  You may not use this
  Software unless you agree to abide by the terms of the License. 
  The License limits your use, and you acknowledge, that the Software may not be 
  modified, copied or distributed unless used solely and exclusively in conjunction 
  with a Texas Instruments Bluetooth device. Other than for the foregoing purpose, 
  you may not use, reproduce, copy, prepare derivative works of, modify, distribute, 
  perform, display or sell this Software and/or its documentation for any purpose.
 
  YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
  PROVIDED �AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
  INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
  NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
  TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
  NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
  LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
  INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
  OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
  OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
  (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 
  Should you have any questions regarding your right to use this Software,
  contact Texas Instruments Incorporated at www.TI.com

 **************************************************************************************************/
package org.udoo.udooblulib.sensor;

//import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import org.udoo.udooblulib.model.IOPin;
import org.udoo.udooblulib.utils.BitUtility;
import org.udoo.udooblulib.utils.Point3D;

import java.util.UUID;

import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_ACC_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_ACC_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_AMB_LIG_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_AMB_LIG_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_BAR_ALTIT_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_BAR_PRESS_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_BAR_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_GYR_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_GYR_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_HUM_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_HUM_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_IOPIN_ANALOG_READ;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_IOPIN_DIGITAL_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_IOPIN_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_KEY_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_KEY_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_MAG_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_MAG_SERV;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_SENSOR_CONF;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_TEM_DATA;
import static org.udoo.udooblulib.sensor.UDOOBLE.UUID_TEM_SERV;

/**
 * This enum encapsulates the differences amongst the sensors. The differences include UUID values and how to interpret the
 * characteristic-containing-measurement.
 */
public enum UDOOBLESensor {

  ACCELEROMETER(UUID_ACC_SERV, UUID_ACC_DATA, UUID_SENSOR_CONF) {
  	@Override
  	public Point3D convert(final byte[] value) {
      float x = (float) ((short) ((value[1] << 8) | (value[0] & 0xff))) * 0.0024244f;
      float y = (float) ((short) ((value[3] << 8) | (value[2] & 0xff))) * 0.0024244f;
      float z = (float) ((short) ((value[5] << 8) | (value[4] & 0xff))) * 0.0024244f;

      return new Point3D(x , y , z );
  	}
  },


  MAGNETOMETER(UUID_MAG_SERV, UUID_MAG_DATA, UUID_SENSOR_CONF) {
    @Override
    public Point3D convert(final byte [] value) {
      // Multiply x and y with -1 so that the values correspond with the image in the app
      float x = (float) ((short) ((value[1] << 8) | (value[0] & 0xff))) / 100f;
	  float y = (float) ((short) ((value[3] << 8) | (value[2] & 0xff))) / 100f;
	  float z = (float) ((short) ((value[5] << 8) | (value[4] & 0xff))) / 100f;

	  return new Point3D(x , y , z);
    }
  },

  GYROSCOPE(UUID_GYR_SERV, UUID_GYR_DATA, UUID_SENSOR_CONF) {
    @Override
    public Point3D convert(final byte [] value) {

      float x = (float) ((short) ((value[1] << 8) | (value[0] & 0xff))) * 0.00625f;
  	  float y = (float) ((short) ((value[3] << 8) | (value[2] & 0xff))) * 0.00625f;
  	  float z = (float) ((short) ((value[5] << 8) | (value[4] & 0xff))) * 0.00625f;

      return new Point3D(x, y, z);
    }
  },

  CAPACITIVE_BUTTON(UUID_KEY_SERV, UUID_KEY_DATA, null) {
    @Override
    public Integer convertKeys(final byte[] value) {
      return (int) value[0];
    }
  },

  TEMPERATURE(UUID_TEM_SERV, UUID_TEM_DATA, UUID_SENSOR_CONF) {
    @Override
    public int convertTemp(final byte[] value) {
      int rbit = (int) Constant.TemperatureTable[0][0];
      float ris = Constant.TemperatureTable[0][1];
      int rBit = (rbit - 8);

      return (int) ((value[1] << rBit | value[0] & rBit) * ris);
    }
  },

  BAROMETER_P(UUID_BAR_SERV, UUID_BAR_PRESS_DATA, UUID_SENSOR_CONF) {
    @Override
    public int convertBar(final byte[] value) {

      int test = ((value[2] << 16) | ((value[1] << 8) & 0xff00) | (value[0]) & 0xff) / 4000;
      return test;

    }
  },

  BAROMETER_A(UUID_BAR_SERV, UUID_BAR_ALTIT_DATA, UUID_SENSOR_CONF) {
    @Override
    public int convertBar(final byte[] value) {
      return ((value[2] << 16) | (value[1] << 8 & 0xff) | (value[0] & 0xffff));
    }
  },

  IOPIN_DIGITAL(UUID_IOPIN_SERV, UUID_IOPIN_DIGITAL_DATA, UUID_SENSOR_CONF) {
    @Override
    public boolean[] convertIOPinDigital(final byte[] value, IOPin... pins) {
      boolean[] iopin = new boolean[pins.length];
      BitUtility.LogBinValue(value, false);
      for (int i = 0; i < pins.length; i++) {
        short pinVal = (short) (1 << pins[i].getPinValue());
        iopin[i] = (value[0] & pinVal) == pinVal;
      }
      return iopin;
    }
  },


  IOPIN_ANALOG(UUID_IOPIN_SERV, UUID_IOPIN_ANALOG_READ, UUID_SENSOR_CONF) {
    @Override
    public float convertADC(final byte[] value) {
      return (float) ((short) ((value[1] << 8) | (value[0] & 0xff)));
    }
  },


  HUMIDITY(UUID_HUM_SERV, UUID_HUM_DATA, UUID_SENSOR_CONF) {
    @Override
    public int convertHumidity(final byte[] value) {
      int rHum = ((short) (value[1] << 8 & 0xf000) | (value[0] & 0xff));
      int rh = 125 * (rHum) / 65536 - 6;
      if (rh < 0)
        rh = 0;
      else if (rh > 100)
        rh = 100;
      return rh;
    }
  },

  AMBIENT_LIGHT(UUID_AMB_LIG_SERV, UUID_AMB_LIG_DATA, UUID_SENSOR_CONF) {
    @Override
    public int convertAmbientLight(final byte[] value) {
      return ((short) ((value[1] << 8) | (value[0] & 0xff)));
    }
  };

  /**
   * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's complement values in the awkward format LSB MSB, which cannot be directly parsed
   * as getIntValue(FORMAT_SINT16, offset) because the bytes are stored in the "wrong" direction.
   *
   * This function extracts these 16 bit two's complement values.
   * */
  private static Integer shortSignedAtOffset(byte[] c, int offset) {
    Integer lowerByte = (int) c[offset] & 0xFF;
    Integer upperByte = (int) c[offset+1]; // // Interpret MSB as signed
    return (upperByte << 8) + lowerByte;
  }

  private static Integer shortUnsignedAtOffset(byte[] c, int offset) {
    Integer lowerByte = (int) c[offset] & 0xFF;
    Integer upperByte = (int) c[offset+1] & 0xFF; // // Interpret MSB as signed
    return (upperByte << 8) + lowerByte;
  }

  public void onCharacteristicChanged(BluetoothGattCharacteristic c) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public Integer convertKeys(byte[] value) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public Point3D convert(byte[] value) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public int convertTemp(byte[] value) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public int convertBar(byte[] value) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public boolean[] convertIOPinDigital(final byte[] value, IOPin... pin){
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public float convertADC(byte[] value) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public int convertHumidity(byte[] value) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

  public int convertAmbientLight(byte[] value) {
    throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
  }

	private final UUID service, data, config;
	private byte enableCode; // See getEnableSensorCode for explanation.
	public static final byte DISABLE_SENSOR_CODE = 0;
	public static final byte ENABLE_SENSOR_CODE = 1;
	public static final byte CALIBRATE_SENSOR_CODE = 2;

  /**
   * Constructor
   * */
  private UDOOBLESensor(UUID service, UUID data, UUID config) {
    this.service = service;
    this.data = data;
    this.config = config;
    this.enableCode = ENABLE_SENSOR_CODE; // This is the sensor enable code for all sensors except the gyroscope
  }

  /**
   * @return the code which, when written to the configuration characteristic, turns on the sensor.
   * */
  public byte getEnableSensorCode() {
    return enableCode;
  }

  public UUID getService() {
    return service;
  }

  public UUID getData() {
    return data;
  }

  public UUID getConfig() {
    return config;
  }

  public static UDOOBLESensor getFromDataUuid(UUID uuid) {
    for (UDOOBLESensor s : UDOOBLESensor.values()) {
      if (s.getData().equals(uuid)) {
        return s;
      }
    }
    throw new RuntimeException("Programmer error, unable to find uuid.");
  }
  
  public static final UDOOBLESensor[] SENSOR_LIST = { ACCELEROMETER, MAGNETOMETER, GYROSCOPE, CAPACITIVE_BUTTON, TEMPERATURE};
}
