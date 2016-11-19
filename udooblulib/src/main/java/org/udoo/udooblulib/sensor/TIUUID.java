package org.udoo.udooblulib.sensor;

import java.util.UUID;

import static java.util.UUID.fromString;

/**
 * Created by harlem88 on 08/11/16.
 */

public class TIUUID {

    public final static UUID

            /* INFO_SERVICE */
            UUID_DEVINFO_SERV = fromString("0000180a-0000-1000-8000-00805f9b34fb"),
            UUID_DEVINFO_FWREV = fromString("00002A26-0000-1000-8000-00805f9b34fb"),

            /* Connection Control */
            UUID_CONN_CTRL_SERV = fromString("f000ccc0-0451-4000-b000-000000000000"),
            UUID_CONN_CTRL_REQ = fromString("f000ccc2-0451-4000-b000-000000000000"),

            /* OAD */
            UUID_OAD_SERV = fromString("f000ffc0-0451-4000-b000-000000000000"),
            UUID_OAD_IMAGE_IDENTIFY = fromString("f000ffc1-0451-4000-b000-000000000000"),
            UUID_OAD_BLOCK_REQUEST = fromString("f000ffc2-0451-4000-b000-000000000000");
}
