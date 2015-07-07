package com.punchthrough.bean.sdk;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.punchthrough.bean.sdk.TestingUtils.LooperRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the BeanManager.
 * Note: This requires an actual bean nearby to pass!
 */
public class TestBeanManager extends AndroidTestCase {

    private LooperRunner lr = new LooperRunner();
    private Thread lrThread = new Thread(lr);
    private BeanManager bm;

    protected void setUp() {
        this.bm = BeanManager.getInstance();
        lrThread.start();
    }

    protected void tearDown() {
        /*
        * "Speaking very generally, never quit() your looper threads. That method exists
        * mostly for historical and testing reasons. In Real Life™, I recommend that you
        * continue to reuse the same looper thread(s) for the life of the process rather
        * than creating/quitting them."
        * */
//        lr.quit();
//        lrThread.join();
    }

    public void testDiscovery() throws InterruptedException {
        final CountDownLatch beanLatch = new CountDownLatch(1);
        final List<Bean> beans = new ArrayList<>();

        BeanDiscoveryListener listener = new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean bean, int rssi) {
                beans.add(bean);
                beanLatch.countDown();
            }

            @Override
            public void onDiscoveryComplete() {
                beanLatch.countDown();
            }
        };

        boolean startedOK = this.bm.startDiscovery(listener);
        assertThat(startedOK).isTrue();
        beanLatch.await();
        assertThat(beans.size()).isGreaterThan(0);
        this.bm.cancelDiscovery();
        for (Bean bean : beans) {
            if (bean.isConnected()) {
                bean.disconnect();
            }
        }
    }
}
