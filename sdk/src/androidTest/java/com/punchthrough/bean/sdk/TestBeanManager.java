package com.punchthrough.bean.sdk;

import android.os.Looper;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the BeanManager.
 * Note: This requires an actual bean nearby to pass!
 */
public class TestBeanManager extends AndroidTestCase {

    private BeanManager bm;

    protected void setUp() {
        this.bm = BeanManager.getInstance();
    }

    protected void tearDown() {
    }

    public void testDiscovery() throws InterruptedException {
        final Looper looper = this.bm.mHandler.getLooper();

        final List<Bean> beans = new ArrayList<>();

        BeanDiscoveryListener listener = new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean bean, int rssi) {
                beans.add(bean);
                looper.quit();
            }

            @Override
            public void onDiscoveryComplete() {
                looper.quit();
            }
        };

        boolean startedOK = BeanManager.getInstance().startDiscovery(listener);
        assertThat(startedOK).isTrue();
        looper.loop();
        assertThat(beans.size()).isGreaterThan(0);
    }
}
