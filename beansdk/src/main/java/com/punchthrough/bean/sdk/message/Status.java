package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import com.punchthrough.bean.sdk.internal.bootloader.State;
import com.punchthrough.bean.sdk.internal.bootloader.Substate;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;

import auto.parcel.AutoParcel;
import okio.Buffer;

import static com.punchthrough.bean.sdk.internal.utility.Misc.bytesToInt;
import static com.punchthrough.bean.sdk.internal.utility.Misc.enumWithRawValue;

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

    public abstract State state();

    public abstract Substate substate();

    public abstract int blocksSent();

    public abstract int bytesSent();

    public static Status create(State state, Substate substate, int blocksSent, int bytesSent) {
        return new AutoParcel_Status(state, substate, blocksSent, bytesSent);
    }

    public static Status fromPayload(Buffer buffer) throws NoEnumFoundException {
        State state = enumWithRawValue(State.class, buffer.readByte());
        Substate substate = enumWithRawValue(Substate.class, buffer.readByte());
        int blocksSent = bytesToInt(buffer.readByte(), buffer.readByte());
        int bytesSent = bytesToInt(buffer.readByte(), buffer.readByte());
        return Status.create(state, substate, blocksSent, bytesSent);
    }

}
