package org.udoo.udooblulib.model;

/**
 * Created by harlem88 on 24/02/16.
 */
public class CharacteristicModel {
    public byte[] value;
    public String uuidStr;
    public String actionType;
    public int status;

    public static CharacteristicModel Builder(String actionType, String uuidStr, byte[] value, int status) {
        CharacteristicModel characteristicModel = new CharacteristicModel();
        characteristicModel.actionType = actionType;
        characteristicModel.uuidStr = uuidStr;
        characteristicModel.value = value;
        characteristicModel.status = status;
        return characteristicModel;
    }
}
