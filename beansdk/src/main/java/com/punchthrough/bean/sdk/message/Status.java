package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.upload.sketch.BeanState;
import com.punchthrough.bean.sdk.internal.upload.sketch.BeanSubstate;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.utility.EnumParse;

import auto.parcel.AutoParcel;
import okio.Buffer;

import static com.punchthrough.bean.sdk.internal.utility.Convert.bytesToInt;

/**
 * Represents a Status message. The Bean sends Status messages to indicate its status has changed.
 *
 * See Bootloader -> Firmware -> Status Response in the App Message Types docs:
 * https://github.com/PunchThrough/bean-documentation/blob/master/app_message_types.md
 *
 * Major ID: 0x10
 * Minor ID: 0x02 (Client -> Bean)
 *           0x82 (Bean -> Client)
 */
@AutoParcel
public abstract class Status implements Parcelable {
    /* AppMessages.h -> BL_MSG_STATUS_T:
     * {
     *     PTD_UINT8 hlState;   // BL_HL_STATE_T
     *     PTD_UINT8 intState;  // BL_STATE_T
     *     PTD_UINT16 blocksSent;
     *     PTD_UINT16 bytesSent;
     * }
     */

    public abstract BeanState beanState();

    public abstract BeanSubstate beanSubstate();

    public abstract int blocksSent();

    public abstract int bytesSent();

    public static Status create(BeanState beanState, BeanSubstate beanSubstate, int blocksSent, int bytesSent) {
        return new AutoParcel_Status(beanState, beanSubstate, blocksSent, bytesSent);
    }

    public static Status fromPayload(Buffer buffer) throws NoEnumFoundException {
        BeanState beanState = EnumParse.enumWithRawValue(BeanState.class, buffer.readByte());
        BeanSubstate beanSubstate = EnumParse.enumWithRawValue(BeanSubstate.class, buffer.readByte());
        int blocksSent = bytesToInt(buffer.readByte(), buffer.readByte());
        int bytesSent = bytesToInt(buffer.readByte(), buffer.readByte());
        return Status.create(beanState, beanSubstate, blocksSent, bytesSent);
    }

}
