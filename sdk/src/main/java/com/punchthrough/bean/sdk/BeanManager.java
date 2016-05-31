/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Little Robots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.punchthrough.bean.sdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Singleton object that provides an interface for discovery Beans.
 */
public class BeanManager {

    // Singleton
    private static BeanManager self = null;

    // Constants
    private static final String TAG = "BeanManager";
    private static final UUID BEAN_UUID = UUID.fromString("a495ff10-c5b1-4b44-b512-1370f02d74de");

    // Dependencies
    private BluetoothAdapter btAdapter;
    private Handler mHandler = new Handler();
    private BeanDiscoveryListener mListener;
    private int scanTimeout = 30;  // Seconds

    // Internal State
    private boolean mScanning = false;
    private HashMap<String, Bean> mBeans = new HashMap<>(32);

    private BeanManager() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private final LeScanCallback mCallback = new LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, final int rssi, byte[] scanRecord) {
            if (isBean(scanRecord)) {

                final Bean bean;

                if (mBeans.containsKey(device.getAddress())) {
                    // We already know about this bean
                    bean = mBeans.get(device.getAddress());
                    if (bean.firmwareUpdateInProgress()) {
                        cancelDiscovery();
                        bean.connect(bean.getLastKnownContext(), bean.getBeanListener());
                    }

                } else {
                    // New Bean
                    bean = new Bean(device);
                    mBeans.put(device.getAddress(), bean);
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onBeanDiscovered(bean, rssi);
                    }
                });
            }
        }
    };

    private Runnable scanTimeoutCallback = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "Scan timeout!");
            cancelDiscovery();
        }
    };

    /**
     * Helper function for starting scan and scheduling the scan timeout
     *
     * @return boolean success flag
     */
    private boolean scan() {

        if (btAdapter.startLeScan(mCallback)) {
            mScanning = true;
            Log.i(TAG, "BLE scan started successfully");

            if (mHandler.postDelayed(scanTimeoutCallback, scanTimeout * 1000)) {
                Log.i(TAG, String.format("Cancelling discovery in %d seconds", scanTimeout));
            } else {
                Log.e(TAG, "Failed to schedule discovery complete callback!");
            }

            return true;
        } else {
            Log.i(TAG, "BLE scan failed!");
            return false;
        }
    }

    /**
     * Set the desired scan timeout in seconds
     *
     * @param timeout seconds
     */
    public void setScanTimeout(int timeout) {
        scanTimeout = timeout;
        Log.i(TAG, String.format("New scan timeout set: %d seconds", scanTimeout));
    }

    /**
     * Get the shared {@link BeanManager} instance.
     *
     * @return The shared BeanManager instance.
     */
    public static synchronized BeanManager getInstance() {
        if (self == null) {
            self = new BeanManager();
        }

        return self;
    }

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * Start discovering nearby Beans. If a discovery is in progress, it will be canceled. A
     * discovery will run for a limited time after which
     * {@link BeanDiscoveryListener#onDiscoveryComplete()} will be called.
     *
     * @param listener the listener for reporting progress
     * @return false if the Bluetooth stack was unable to start the scan.
     */
    public boolean startDiscovery(BeanDiscoveryListener listener) {
        if (mScanning) {
            Log.e(TAG, "Already discovering");
            return true;
        }

        mListener = listener;
        return scan();
    }

    /**
     * Start discovering nearby Beans using an existing BeanListener.
     *
     * Currently this function is only used by OADProfile to start scanning after
     * the Bean disconnects during the OAD process.
     */
    public boolean startDiscovery() {
        if (mScanning) {
            Log.e(TAG, "Already discovering");
            return true;
        }

        if (mListener == null) {
            throw new NullPointerException("Listener cannot be null");
        }

        return scan();
    }

    /**
     * Cancel a scan currently in progress. If no scan is in progress, this method does nothing.
     */
    public void cancelDiscovery() {
        mHandler.removeCallbacks(scanTimeoutCallback);

        if (mScanning) {
            Log.i(TAG, "Cancelling discovery process");
            BluetoothAdapter.getDefaultAdapter().stopLeScan(mCallback);
            mScanning = false;
            boolean success = mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onDiscoveryComplete();
                }
            });
            if (!success) {
                Log.e(TAG, "Failed to post Discovery Complete callback!");
            }
        } else {
            Log.e(TAG, "No discovery in progress");
        }
    }

    /**
     * Return the Beans found since the last scan started.
     *
     * @return a collection of Beans found
     */
    public Collection<Bean> getBeans() {
        return new ArrayList<>(mBeans.values());
    }

    /**
     * Clear the Beans that this BeanManager has discovered.
     */
    public void forgetBeans() {
        mBeans.clear();
    }

    /**
     * Determines if a {@link android.bluetooth.BluetoothDevice} is a Bean based on its scan record
     * value.
     *
     * @param scanRecord    The scanRecord provided by
     *                      {@link android.bluetooth.BluetoothAdapter.LeScanCallback}
     * @return              true if device is a Bean
     */
    private boolean isBean(byte[] scanRecord) {
        List<UUID> uuids = parseUUIDs(scanRecord);
        return uuids.contains(BEAN_UUID);
    }

    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                                    offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }
}
