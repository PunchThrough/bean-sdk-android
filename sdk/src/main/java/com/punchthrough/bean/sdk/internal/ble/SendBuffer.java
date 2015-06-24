package com.punchthrough.bean.sdk.internal.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.punchthrough.bean.sdk.message.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SendBuffer {

    private static final String TAG = "SendBuffer";
    public static final int SEND_INTERVAL = 5;  // ms

    private final GattClient gattClient;
    private final BluetoothGattCharacteristic charc;
    private final List<byte[]> packets = new ArrayList<>();
    private final List<Integer> ids = new ArrayList<>();
    private final Callback<Integer> onPacketSent;

    private Timer sendTimer = new Timer();
    private int retries;

    /**
     * Set up a send buffer to manage outgoing packets for a specific
     * {@link android.bluetooth.BluetoothGatt} device and
     * {@link android.bluetooth.BluetoothGattCharacteristic}.
     *  @param gattClient          The device associated with the characteristic
     * @param charc         The characteristic to send packets to
     * @param onPacketSent  Called with the ID of a packet when that packet is sent successfully
     */
    public SendBuffer(GattClient gattClient, BluetoothGattCharacteristic charc,
                      Callback<Integer> onPacketSent) {
        this.gattClient = gattClient;
        this.charc = charc;
        this.onPacketSent = onPacketSent;
    }

    /**
     * Add a packet to the buffer to be sent. If no other packets are in the buffer, it will be sent
     * immediately.
     *
     * @param data  The packet to be sent
     * @param id    The ID of the packet to be used in debug messages and callbacks
     */
    public void send(byte[] data, int id) {
        boolean isFirstPacket = (packets.size() == 0);
        packets.add(data);
        ids.add(id);
        if (isFirstPacket) {
            scheduleSendTask(true);
        }
        Log.d(TAG, "Added packet " + id + " to buffer; " + packets.size() + " packets in buffer");
    }

    /**
     * Schedules the send task to run either immediately or at SEND_INTERVAL.
     *
     * @param runNow true runs the task immediately, false schedules it for SEND_INTERVAL ms from
     *               now
     */
    private void scheduleSendTask(boolean runNow) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {

                byte[] packet;

                try {
                    packet = packets.get(0);

                } catch (IndexOutOfBoundsException e) {
                    // No packets left; return without scheduling another run
                    return;

                }

                charc.setValue(packet);
                boolean result = gattClient.writeCharacteristic(charc);

                if (result) {
                    packets.remove(0);
                    int id = ids.remove(0);
                    retries = 0;

                    if (onPacketSent != null) {
                        onPacketSent.onResult(id);
                    }
                    Log.d(TAG, "Packet " + id + " sent after " + retries + " retries");

                } else {
                    retries++;

                }

                scheduleSendTask(false);
            }
        };

        if (runNow) {
            task.run();

        } else {
            sendTimer.schedule(task, SEND_INTERVAL);

        }
    }

}
