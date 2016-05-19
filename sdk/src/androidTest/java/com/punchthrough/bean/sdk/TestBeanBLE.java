package com.punchthrough.bean.sdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.punchthrough.bean.sdk.internal.utility.Constants;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Low-level BLE tests that do not make use of the SDK
 *
 * Run this test if you just want to verify that BLE is working for a given phone and Bean
 *
 * These tests do the following:
 *   - Scan/Discover
 *   - Connect to Device
 *   - Discover services
 *   - Read HW version from Device Info Service
 *   - Enable notifications on OAD Identify char
 *   - Write 0x0000 to OAD Identify char
 *   - Receive notification
 *
 */
public class TestBeanBLE extends AndroidTestCase {

    private final String TAG = "TestBeanBLE";

    // Dependencies
    private Handler handler;
    private BluetoothAdapter bleAdapter;

    // Latches for synchronous testing
    private final CountDownLatch discoveryLatch = new CountDownLatch(1);
    private final CountDownLatch gattConnectLatch = new CountDownLatch(1);
    private final CountDownLatch discoverServicesLatch = new CountDownLatch(1);
    private final CountDownLatch readHardwareVersionLatch = new CountDownLatch(1);
    private final CountDownLatch notificationIdentifyLatch = new CountDownLatch(1);

    // Internal state
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private String targetAddress = "C4:BE:84:49:BD:3C";  // Change this
    private int lastKnownGattState;
    private String hardwareVersionString = null;
    private byte[] notificationIdentifyValue = null;

    public void setUp() {
        this.handler = new Handler();
        this.bleAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, final int rssi, byte[] scanRecord) {
            if (device.getAddress().equals(targetAddress)) {
                devices.add(device);
                discoveryLatch.countDown();
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            lastKnownGattState = newState;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gattConnectLatch.countDown();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                discoverServicesLatch.countDown();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(Constants.UUID_DEVICE_INFO_CHAR_HARDWARE_VERSION)) {
                    hardwareVersionString = characteristic.getStringValue(0);
                    readHardwareVersionLatch.countDown();
                }
            }
        }

        @Override
        public void onReliableWriteCompleted (BluetoothGatt gatt, int status) {
            Log.i(TAG, "onReliableWriteCompleted: " + status);
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite: " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(Constants.UUID_OAD_CHAR_IDENTIFY)) {
                notificationIdentifyValue = characteristic.getValue();
                notificationIdentifyLatch.countDown();
            }
        }
    };

    private void refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                localMethod.invoke(localBluetoothGatt, new Object[0]);
            } else {
                Log.e(TAG, "Couldn't find local method: refresh");
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device");
        }
    }

    private BluetoothDevice findDevice() throws InterruptedException {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bleAdapter.stopLeScan(leScanCallback);
            }
        }, 10000);

        bleAdapter.startLeScan(leScanCallback);
        discoveryLatch.await(10, TimeUnit.SECONDS);
        if (devices.size() < 1) {
            fail("FAILURE: No Device Found");
        }

        BluetoothDevice device = devices.get(devices.size() - 1);
        Log.i(TAG, "SUCCESS: Found device " + device.getAddress());
        return device;
    }

    private BluetoothGatt connectGatt(BluetoothDevice device) throws InterruptedException {
        BluetoothGatt gatt = device.connectGatt(getContext(), false, gattCallback);
        gattConnectLatch.await(10, TimeUnit.SECONDS);
        if (lastKnownGattState != BluetoothProfile.STATE_CONNECTED) {
            fail("FAILURE: Gatt didn't connect!");
        }

        Log.i(TAG, "SUCCESS: Gatt connected");
        return gatt;
    }

    private void discoverServices(BluetoothGatt gatt) throws InterruptedException {
        gatt.discoverServices();
        discoverServicesLatch.await(20, TimeUnit.SECONDS);
        if (discoverServicesLatch.getCount() > 0) {
            fail("FAILURE: Couldn't discover services");
        }

        Log.i(TAG, "SUCCESS: Services discovered");
    }

    private void readHardwareVersion(BluetoothGatt gatt) throws InterruptedException {

        BluetoothGattService dis = gatt.getService(Constants.UUID_DEVICE_INFO_SERVICE);
        BluetoothGattCharacteristic hwv = dis.getCharacteristic(Constants.UUID_DEVICE_INFO_CHAR_HARDWARE_VERSION);
        if (hwv == null) {
            fail("FAILURE: No characteristic Hardware Version");
        }
        if (!gatt.readCharacteristic(hwv)) {
            fail("FAILURE: Read char failed Hardware Version");
        }
        readHardwareVersionLatch.await(10, TimeUnit.SECONDS);
        if (hardwareVersionString == null) {
            fail("FAILURE: No hardware version");
        }

        Log.i(TAG, "SUCCESS: Read hardware Version " + hardwareVersionString);
    }

    private void enableNotificationOADIdentify(BluetoothGatt gatt) {
        BluetoothGattService oads = gatt.getService(Constants.UUID_OAD_SERVICE);
        BluetoothGattCharacteristic iden = oads.getCharacteristic(Constants.UUID_OAD_CHAR_IDENTIFY);
        if (!gatt.setCharacteristicNotification(iden, true)) {
            Log.e(TAG, "FAILURE: Set Characteristic Notification");
        }
        BluetoothGattDescriptor descriptor = iden.getDescriptor(Constants.UUID_CLIENT_CHAR_CONFIG);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(descriptor)) {
            Log.e(TAG, "FAILURE: Set Write Descriptor");
        }
        Log.i(TAG, "SUCCESS: Notification enabled");
    }

    private void writeCharacteristicOADIdentify(BluetoothGatt gatt) {
        BluetoothGattService oads = gatt.getService(Constants.UUID_OAD_SERVICE);
        BluetoothGattCharacteristic iden = oads.getCharacteristic(Constants.UUID_OAD_CHAR_IDENTIFY);
        byte[] zeros = new byte[2];
        zeros[0] = 0;
        zeros[1] = 0;
        iden.setValue(zeros);
        if (!gatt.writeCharacteristic(iden)) {
            Log.e(TAG, "FAILURE: Write char OAD Identify 0x0000");
            fail("FAILURE: Write char OAD Identify 0x0000");
        }
        Log.i(TAG, "SUCCESS: Wrote characteristic");
    }

    private void receiveNotificationOADIdentify() throws InterruptedException {
        notificationIdentifyLatch.await(10, TimeUnit.SECONDS);
        if (notificationIdentifyValue == null) {
            fail("FAILURE: Didn't receive notification");
        }
        Log.i(TAG, "SUCCESS: Received notification " + notificationIdentifyValue.toString());
    }

    @Suppress
    public void testBLE() throws InterruptedException {

        // Scan/Discover
        BluetoothDevice device = findDevice();

        // Connect to closest device (hopefully a Bean)
        BluetoothGatt gatt = connectGatt(device);

        // Clear Android Cache
        refreshDeviceCache(gatt);

        // Discover services
        discoverServices(gatt);

        // Read HW version from Device Info Service
        readHardwareVersion(gatt);

        // Enable notifications on OAD Identify char
        enableNotificationOADIdentify(gatt);

        // Write 0x0000 to OAD Identify char
        writeCharacteristicOADIdentify(gatt);

        // Receive notification
        receiveNotificationOADIdentify();

        gatt.disconnect();
        gatt.close();

    }
}
