package com.punchthrough.bean.sdk;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

public class BeanTest {

    // Mocks
    BluetoothDevice mockDevice;
    Handler mockHandler;

    // Class under test
    Bean bean;

    @Before
    public void setup() {
        mockDevice = mock(BluetoothDevice.class);
        mockHandler = mock(Handler.class);
        bean = new Bean(mockDevice, mockHandler);
    }

    @Test
    public void testBeanConnection() {
        assertThat(bean.isConnected()).isFalse();
    }

}
