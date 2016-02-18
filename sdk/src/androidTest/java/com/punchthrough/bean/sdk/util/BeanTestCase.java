package com.punchthrough.bean.sdk.util;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.BeanManager;

public class BeanTestCase extends AndroidTestCase {

    private TestingUtils.LooperRunner lr = new TestingUtils.LooperRunner(BeanManager.getInstance().getHandler().getLooper());

    protected void setUp() {
        lr.start();
    }

    protected void tearDown() throws InterruptedException {}

}
