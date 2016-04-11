package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.util.BeanTestCase;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;


public class TestBeanAutoReconnect extends BeanTestCase {

    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private final CountDownLatch connectLatch2 = new CountDownLatch(1);
    private final CountDownLatch disconnectLatch = new CountDownLatch(1);
    private final CountDownLatch disconnectLatch2 = new CountDownLatch(1);

    private BeanListener beanListener = new BeanListener() {
        @Override
        public void onConnected() {
            System.out.println("Bean Connected");

            // This callback should be called twice, we need to countDown the appropriate latch
            if (connectLatch.getCount() > 0 ) {
                connectLatch.countDown();
            } else {
                connectLatch2.countDown();
            }
        }

        @Override
        public void onConnectionFailed() {}

        @Override
        public void onDisconnected() {
            System.out.println("Bean Disconnected");

            // This callback should be called twice, we need to countDown the appropriate latch
            if (disconnectLatch.getCount() > 0) {
                disconnectLatch.countDown();
            } else {
                disconnectLatch2.countDown();
            }
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {}

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {}

        @Override
        public void onError(BeanError error) {}

        @Override
        public void onReadRemoteRssi(int rssi) {}
    };

    public void testBeanAutoReconnect() throws Exception {

        // Scan for and retrieve a Bean without connecting to it
        Bean bean = discoverBean();

        // Set it to auto reconnect
        bean.setAutoReconnect(true);

        // It shouldn't be connected
        assertThat(bean.isConnected()).isFalse();

        // Start async connection process
        bean.connect(getContext(), beanListener);

        // Wait for connection
        ensureConnected(bean, connectLatch);
        assertThat(bean.isConnected()).isTrue();

        // Start async disconnection process
        bean.disconnect();

        // Wait for disconnection
        ensureDisconnected(bean, disconnectLatch);
        assertThat(bean.isConnected()).isFalse();

        // Scan for and retrieve our test bean, without connecting to it explicitly
        discoverBean();

        // It should be connected WITHOUT calling .connect() again!
        ensureConnected(bean, connectLatch2);
        assertThat(bean.isConnected()).isTrue();

        // Always disconnect at end of test so that other tests will pass
        bean.disconnect();
        ensureDisconnected(bean, disconnectLatch2);
    }
}