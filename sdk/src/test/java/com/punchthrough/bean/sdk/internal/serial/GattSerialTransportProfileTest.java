package com.punchthrough.bean.sdk.internal.serial;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;

import com.punchthrough.bean.sdk.UnitTestUtils;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.EnumParse;
import com.punchthrough.bean.sdk.message.ScratchBank;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GattSerialTransportProfileTest {

    // Mocks
    BluetoothGattService mockSerialService;
    BluetoothGattService mockScratchService;
    BluetoothGattCharacteristic mockChar;
    GattClient mockGattClient;
    Handler mockHandler;
    GattSerialTransportProfile.SerialListener mockListener;

    // Class under test
    GattSerialTransportProfile gstp;

    private GattSerialMessage buildMessage(int messageId, byte[] payload) {
        Buffer buf = new Buffer();
        buf.writeByte((messageId >> 8) & 0xff);
        buf.writeByte(messageId & 0xff);
        for (byte b : payload) {
            buf.writeByte(b);
        }
        return GattSerialMessage.fromPayload(buf.readByteArray());
    }

    @Before
    public void setup() {
        mockGattClient = mock(GattClient.class);
        mockHandler = UnitTestUtils.ImmediatelyRunningHandler();
        mockListener = mock(GattSerialTransportProfile.SerialListener.class);
        mockSerialService = mock(BluetoothGattService.class);
        mockScratchService = mock(BluetoothGattService.class);
        mockChar = mock(BluetoothGattCharacteristic.class);
        when(mockGattClient.getService(Constants.UUID_SERIAL_SERVICE)).thenReturn(mockSerialService);
        when(mockGattClient.getService(Constants.UUID_SCRATCH_SERVICE)).thenReturn(mockScratchService);
        when(mockSerialService.getCharacteristic(Constants.UUID_SERIAL_CHAR)).thenReturn(mockChar);
        final List<BluetoothGattCharacteristic> chars = new ArrayList<>();
        chars.add(mockChar);
        when(mockSerialService.getCharacteristics()).thenReturn(chars);
        gstp = new GattSerialTransportProfile(mockGattClient, mockHandler);
    }

    @Test
    public void testOnCharacteristicChanged() throws NoEnumFoundException {
        // More setup
        byte[] value = {1, 2, 3};
        int index = 1;
        ScratchBank bank = EnumParse.enumWithRawValue(ScratchBank.class, index);
        gstp.setListener(mockListener);
        BluetoothGattCharacteristic mockChar = mock(BluetoothGattCharacteristic.class);
        when(mockChar.getUuid()).thenReturn(Constants.UUID_SCRATCH_CHAR_1);
        when(mockChar.getValue()).thenReturn(value);

        // Test and verify
        gstp.onCharacteristicChanged(mockGattClient, mockChar);
        verify(mockListener).onScratchValueChanged(bank, value);
    }

    @Test
    public void testSendSinglePacketMessage() {
        int msgId = 0x0102;
        byte[] payload = new byte[] {0, 1, 2, 3};

        final List<byte[]> packets = new ArrayList<>();
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                packets.add((byte[])args[0]);
                return null;
            }
        }).when(mockChar).setValue((byte[]) any());

        gstp.onProfileReady();
        GattSerialMessage msg = buildMessage(msgId, payload);
        gstp.sendMessage(msg.getBuffer());
        assertThat(packets.size()).isEqualTo(1);
    }

}
