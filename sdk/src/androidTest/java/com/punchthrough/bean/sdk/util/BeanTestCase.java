package com.punchthrough.bean.sdk.util;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.util.TestingUtils.AsyncUtils;

public class BeanTestCase extends AndroidTestCase {

    protected void setUp() {
        AsyncUtils.startLooper(BeanManager.getInstance().getHandler().getLooper());
    }

    protected void tearDown() throws InterruptedException {
        AsyncUtils.stopLooper();
    }

}
