package com.punchthrough.bean.sdk.util;

import android.os.Looper;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This package contains helper classes to facilitate testing
 */

public class TestingUtils {

    private static class LooperRunner implements Runnable {

        private Looper looper;

        public LooperRunner(Looper looper) {
            this.looper = looper;
        }

        public void run() {
            this.looper.prepare();
            this.looper.loop();
        }

        public void quit() {
            this.looper.quitSafely();
        }
    }

    public static class AsyncUtils {

        private static LooperRunner lr;
        private static Thread lrThread;

        public static void startLooper(Looper looper) {
            lr = new LooperRunner(looper);
            lrThread = new Thread(lr);
            lrThread.start();
        }

        public static void stopLooper() throws InterruptedException {
            lr.quit();
            lrThread.join();
        }
    }

    public static class BeanUtils {

        public static List<Bean> getBeans(int num) throws InterruptedException {
            final CountDownLatch beanLatch = new CountDownLatch(num);
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

            boolean startedOK = BeanManager.getInstance().startDiscovery(listener);
            assertThat(startedOK).isTrue();
            beanLatch.await(60, TimeUnit.SECONDS);
            assertThat(beans.size()).isEqualTo(num);
            return beans;
        }

        public static Bean getBeanByName(String name) throws Exception {
            final CountDownLatch beanLatch = new CountDownLatch(1);
            final List<Bean> beans = new ArrayList<>();

            final String targetName = name;

            BeanDiscoveryListener listener = new BeanDiscoveryListener() {
                @Override
                public void onBeanDiscovered(Bean bean, int rssi) {
                    if (bean.getDevice().getName().equals(targetName)) {
                        System.out.println("[BeanUtils] Found Bean by name: " + targetName);
                        beans.add(bean);
                        beanLatch.countDown();
                    }
                }

                @Override
                public void onDiscoveryComplete() {
                    System.out.println("[BeanUtils] Discovery Complete!");
                    beanLatch.countDown();
                }
            };

            boolean startedOK = BeanManager.getInstance().startDiscovery(listener);
            assertThat(startedOK).isTrue();
            beanLatch.await(60, TimeUnit.SECONDS);
            if (beans.isEmpty()) {
                throw new Exception("No bean named: " + name);
            }
            return beans.get(0);
        }

    }
}
