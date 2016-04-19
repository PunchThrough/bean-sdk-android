package com.punchthrough.bean.sdk;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.content.Context;

import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.serial.GattSerialTransportProfile.SerialListener;
import com.punchthrough.bean.sdk.internal.serial.GattSerialTransportProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

public class BeanTest {

    GattClient.ConnectionListener testListener;
    List<Runnable> handlerRunnables = new ArrayList<>();

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
                testListener = (GattClient.ConnectionListener) args[0];
                return null;
            }
        }).when(mockGattClient).setListener(any(GattClient.ConnectionListener.class));

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                handlerRunnables.add((Runnable) args[0]);
                return null;
            }
        }).when(mockHandler).post(any(Runnable.class));

        // Instantiate class under test
        bean = new Bean(mockDevice, mockGattClient, mockHandler);
    }

    @Test
    public void testBeanConnection() {
        BeanListener mockListener = mock(BeanListener.class);
        bean.connect(mockContext, mockListener);
        testListener.onConnected();
        for (Runnable r : handlerRunnables) {
            r.run();
        }
        verify(mockListener).onConnected();
    }

}
