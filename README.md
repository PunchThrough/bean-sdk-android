# LightBlue Bean SDK for Android

Punch Through's SDK for speeding up development with the LightBlue Bean development platform. Build Android apps that talk to your Beans.

* [Bean SDK API Documentation](http://punchthrough.github.io/Bean-Android-SDK/)
* [Bean SDK listing at Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.punchthrough.bean.sdk%22)

# Install

If you're using Android Studio, you can add the Bean SDK to your project by adding a dependency in your Gradle build file.

Your project's Gradle build file is located at `/path/to/YOUR_PROJECT_HERE/app/build.gradle`. Note that this file is inside the `app` folder.

Inside your Gradle build file, add the Bean SDK to the `dependencies` block:

```groovy
dependencies {
    ...
    compile 'com.punchthrough.bean.sdk:sdk:1.0.1'
    ...
}
```

Then sync with Gradle and Android Studio will install the Bean SDK from Maven Central.

# Use the SDK

Check out our [Bean SDK API Documentation](http://punchthrough.github.io/Bean-Android-SDK/) to learn more about the Bean SDK's available methods.

## List nearby Bean(s)

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

## Read device information

The following snippet will connect to a `Bean` and read it's device information using the
Device Information Service (DIS) BLE profile.

```java
// Assume we have a reference to the 'beans' ArrayList from above.
final Bean bean = beans[0];

BeanListener beanListener = new BeanListener() {
    @Override
    public void onConnected() {
        System.out.println("connected to Bean!");
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

# Developing and Contributing

Check out [our HACKING file](HACKING.md) to read our developer's guide to the SDK.
