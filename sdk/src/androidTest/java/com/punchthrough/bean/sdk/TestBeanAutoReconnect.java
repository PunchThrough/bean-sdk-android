package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.util.BeanTestCase;
import com.punchthrough.bean.sdk.util.TestingUtils.BeanUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class TestBeanAutoReconnect extends BeanTestCase {

    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private final CountDownLatch disconnectLatch = new CountDownLatch(1);
    private final CountDownLatch reconnectLatch = new CountDownLatch(1);

    private BeanListener beanListener = new BeanListener() {
        @Override
        public void onConnected() {
            System.out.println("Bean Connected");

            if (connectLatch.getCount() > 0 ) {
                // Should be the first time this callback has been called
                connectLatch.countDown();
            } else {
                // Should be the second time this callback has been called
                reconnectLatch.countDown();
            }
        }

        @Override
        public void onConnectionFailed() {}

        @Override
        public void onDisconnected() {
            System.out.println("Bean Disconnected");
            disconnectLatch.countDown();
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {}

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {}

        @Override
        public void onError(BeanError error) {}
    };

    private void waitForConnect() throws InterruptedException {
        connectLatch.await(20, TimeUnit.SECONDS);
    }

    private void waitForDisconnect() throws InterruptedException {
        disconnectLatch.await(20, TimeUnit.SECONDS);
    }

    private void waitForReconnect() throws InterruptedException {
        reconnectLatch.await(20, TimeUnit.SECONDS);
    }

    public void testBeanAutoReconnect() throws Exception {

        // Scan for and retrieve a Bean without connecting to it
        Bean bean = BeanUtils.getBeanByName("TESTBEAN");

        // Set it to auto reconnect
        bean.setAutoReconnect(true);

        // It shouldn't be connected
        assertThat(bean.isConnected()).isFalse();

        // Start async connection process
        bean.connect(getContext(), beanListener);

        // Wait for connection
        waitForConnect();
        assertThat(bean.isConnected()).isTrue();

        // Start async disconnection process
        bean.disconnect();

        // Wait for disconnection
        waitForDisconnect();
        assertThat(bean.isConnected()).isFalse();

        // Scan for and retrieve our test bean, without connecting to it explicitly
        BeanUtils.getBeanByName("TESTBEAN");

        // It should be connected WITHOUT calling .connect() again!
        waitForReconnect();
        assertThat(bean.isConnected()).isTrue();

    }
}