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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A thin wrapper to discover nearby Beans.
 */
public class BeanManager {
    private static final String TAG = "BeanManager";
    private static final UUID BEAN_UUID = UUID.fromString("a495ff10-c5b1-4b44-b512-1370f02d74de");
    private static final long SCAN_TIMEOUT = 30000;
    private static BeanManager sInstance = new BeanManager();
    protected Handler mHandler = new Handler();
    private BeanDiscoveryListener mListener;
    private boolean mScanning = false;
    private Runnable mCompleteDiscoveryCallback = new Runnable() {
        @Override
        public void run() {
            completeDiscovery();
        }
    };
    private final LeScanCallback mCallback = new LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, final int rssi, byte[] scanRecord) {
            if (!mBeans.containsKey(device.getAddress()) && isBean(scanRecord)) {
                mHandler.removeCallbacks(mCompleteDiscoveryCallback);
                final Bean bean = new Bean(device);
                mBeans.put(device.getAddress(), bean);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onBeanDiscovered(bean, rssi);
                    }
                });
                mHandler.postDelayed(mCompleteDiscoveryCallback, SCAN_TIMEOUT / 2);
            }
        }
    };
    private HashMap<String, Bean> mBeans = new HashMap<>(32);

    private BeanManager() {
    }

    /**
     * Get the shared {@link BeanManager} instance.
     *
     * @return The shared BeanManager instance.
     */
    public static BeanManager getInstance() {
        return sInstance;
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
        if (listener == null) {
            throw new NullPointerException("Listener cannot be null");
        }
        if (mScanning) {
            cancelDiscovery();
        }
        mBeans.clear();
        mListener = listener;
        mScanning = true;
        if (BluetoothAdapter.getDefaultAdapter().startLeScan(mCallback)) {
            mHandler.postDelayed(mCompleteDiscoveryCallback, SCAN_TIMEOUT);
            return true;
        }
        return false;
    }

    /**
     * Cancel a scan currently in progress. If no scan is in progress, this method does nothing.
     */
    public void cancelDiscovery() {
        if (mScanning) {
            BluetoothAdapter.getDefaultAdapter().stopLeScan(mCallback);
            mScanning = false;
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
     * Finish Bean discovery.
     */
    private void completeDiscovery() {
        if (mScanning) {
            BluetoothAdapter.getDefaultAdapter().stopLeScan(mCallback);
            mScanning = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onDiscoveryComplete();
                }
            });
        }
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
