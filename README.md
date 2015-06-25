# [PRE-RELEASE] LightBlue Bean Android SDK

This software is still under heavy development and design, use at your own risk!

# Learn!

***

### List nearby Bean(s)

```java
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

BeanManager.getInstance().startDiscovery(listener);
java.lang.Thread.sleep(5000)
for (Bean bean : beans) {
    System.out.println(bean.getName())
}

```
