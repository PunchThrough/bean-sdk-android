package com.punchthrough.bean.sdk;

import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.util.TestingUtils;
import com.punchthrough.bean.sdk.util.TestingUtils.BeanUtils;

import android.test.AndroidTestCase;

import java.sql.Time;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class TestBeanAutoReconnect extends AndroidTestCase {

    private TestingUtils.LooperRunner lr = new TestingUtils.LooperRunner(BeanManager.getInstance().mHandler.getLooper());
    private Thread lrThread = new Thread(lr);

    protected void setUp() {
        lrThread.start();
    }

    protected void tearDown() {}

    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private final CountDownLatch disconnectionLatch = new CountDownLatch(1);
    private final CountDownLatch reconnectionLatch = new CountDownLatch(1);

    private BeanListener beanListener = new BeanListener() {
        @Override
        public void onConnected() {
            System.out.println("Bean Connected");

            if (connectionLatch.getCount() > 0 ) {
                // Should be the first time this callback has been called
                connectionLatch.countDown();
            } else {
                // Should be the second time this callback has been called
                reconnectionLatch.countDown();
            }

        }

        @Override
        public void onConnectionFailed() {

        }

        @Override
        public void onDisconnected() {
            System.out.println("Bean Disconnected");
            disconnectionLatch.countDown();
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {

        }

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {

        }

        @Override
        public void onError(BeanError error) {

        }
    };

    private void waitForConnected() throws InterruptedException {
        connectionLatch.await(20, TimeUnit.SECONDS);
    }

    private void waitForDisconnected() throws InterruptedException {
        disconnectionLatch.await(20, TimeUnit.SECONDS);
    }

    private void waitForReconnect() throws InterruptedException {
        reconnectionLatch.await(20, TimeUnit.SECONDS);
    }

    public void testBeanAutoReconnect() throws Exception {

        // Get a Bean
        Bean bean = BeanUtils.getBeanByName("TESTBEAN");

        // Set it to auto reconnect
        bean.setAutoReconnect(true);

        // It shouldn't be connected
        assertThat(bean.isConnected()).isFalse();

        // Start async connection process
        bean.connect(getContext(), beanListener);

        // Wait for connection
        waitForConnected();
        assertThat(bean.isConnected()).isTrue();

        // Start async disconnection process
        bean.disconnect();

        // Wait for disconnection
        waitForDisconnected();
        assertThat(bean.isConnected()).isFalse();

        // Get the same Bean
        Bean beanAgain = BeanUtils.getBeanByName("TESTBEAN");

        // It should be connected WITHOUT calling .connect() again!
        waitForReconnect();
        assertThat(beanAgain.isConnected()).isTrue();

    }
}