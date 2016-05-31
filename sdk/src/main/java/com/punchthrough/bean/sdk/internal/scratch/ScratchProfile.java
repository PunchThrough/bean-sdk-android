package com.punchthrough.bean.sdk.internal.scratch;

import com.punchthrough.bean.sdk.internal.ble.BaseProfile;
import com.punchthrough.bean.sdk.internal.ble.GattClient;

public class ScratchProfile extends BaseProfile {

    protected static final String TAG = "ScratchProfile";
    private boolean ready = false;

    public ScratchProfile(GattClient client) {
        super(client);
    }

    public String getName() {
        return TAG;
    }

    public boolean isReady() {
        return ready;
    }

    public void clearReady() {
        ready = false;
    }

}
