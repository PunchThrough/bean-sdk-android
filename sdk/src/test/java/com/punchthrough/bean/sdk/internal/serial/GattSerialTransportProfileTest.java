package com.punchthrough.bean.sdk.internal.serial;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;

import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.EnumParse;
import com.punchthrough.bean.sdk.message.ScratchBank;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GattSerialTransportProfileTest {

    // Mocks
    GattClient mockGattClient;
    Handler mockHandler;
    GattSerialTransportProfile.SerialListener mockListener;

    // Class under test
    GattSerialTransportProfile gstp;

    @Before
    public void setup() {
        mockGattClient = mock(GattClient.class);
        mockHandler = mock(Handler.class);
        mockListener = mock(GattSerialTransportProfile.SerialListener.class);
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
}
