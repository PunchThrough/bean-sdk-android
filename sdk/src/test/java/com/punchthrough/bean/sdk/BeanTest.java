package com.punchthrough.bean.sdk;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.content.Context;

import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.serial.GattSerialTransportProfile.Listener;
import com.punchthrough.bean.sdk.internal.serial.GattSerialTransportProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class BeanTest {

    Listener testListener;

    // Mocks
    Context mockContext;
    BluetoothDevice mockDevice;
    Handler mockHandler;
    GattClient mockGattClient;
    GattSerialTransportProfile mockGattSerialTransportProfile;

    // Class under test
    Bean bean;

    @Before
    public void setup() {

        // Instantiate mocks
        mockContext = mock(Context.class);
        mockDevice = mock(BluetoothDevice.class);
        mockHandler = mock(Handler.class);
        mockGattClient = mock(GattClient.class);
        mockGattSerialTransportProfile = mock(GattSerialTransportProfile.class);

        // Customize some behavior
        when(mockGattClient.getSerialProfile()).thenReturn(mockGattSerialTransportProfile);

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                testListener = (Listener) args[0];
                return null;
            }
        }).when(mockGattSerialTransportProfile).setListener(any(Listener.class));

        // Instantiate class under test
        bean = new Bean(mockDevice, mockGattClient, mockHandler);
    }

    @Test
    public void testBeanConnection() {
        // It should start disconnected
        assertThat(bean.isConnected()).isFalse();

        // Issue connection
        bean.connect(mockContext, mock(BeanListener.class));
        testListener.onConnected();

        // Connected!
        assertThat(bean.isConnected()).isTrue();

    }

}
