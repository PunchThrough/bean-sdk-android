# [PRE-RELEASE] LightBlue Bean Android SDK

This software is still under heavy development and design, use at your own risk!

* [Javadocs](http://punchthrough.github.io/Bean-Android-SDK/docs/javadoc/index.html)

# Learn!

### List nearby Bean(s)

This snippet makes use of the `BeanManager` and the Listener pattern to provide callbacks
when Beans are discovered and when the discovery process is complete.

```java
final List<Bean> beans = new ArrayList<>();

BeanDiscoveryListener listener = new BeanDiscoveryListener() {
    @Override
    public void onBeanDiscovered(Bean bean, int rssi) {
        beans.add(bean);
    }

    @Override
    public void onDiscoveryComplete() {
        for (Bean bean : beans) {
            System.out.println(bean.getDevice().getName());   // "Bean"              (example)
            System.out.println(bean.getDevice().mAddress);    // "B4:99:4C:1E:BC:75" (example)
        }

    }
};

BeanManager.getInstance().startDiscovery(listener);

```

### Read device information

The following snippet will connect to a `Bean` and read it's device information using the
Device Information Service (DIS) BLE profile.

```java
// Assume we have a reference to the 'beans' ArrayList from above.
final Bean bean = beans[0];

BeanListener beanListener = new BeanListener() {
    @Override
    public void onConnected() {
        System.out.prinln("connected to Bean!");
        bean.readDeviceInfo(new Callback<DeviceInfo>() {
            @Override
            public void onResult(DeviceInfo deviceInfo) {
                System.out.println(deviceInfo.hardwareVersion());
                System.out.println(deviceInfo.firmwareVersion());
                System.out.println(deviceInfo.softwareVersion());
            }
        });
    }

    // In practice you must implement the other Listener methods
    ...
};

// Assuming you are in an Activity, use 'this' for the context
bean.connect(this, beanListener);

```

### Loading a Sketch

Coming soon!
