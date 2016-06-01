# LightBlue Bean SDK for Android

[![Circle CI](https://circleci.com/gh/PunchThrough/bean-sdk-android/tree/master.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/PunchThrough/bean-sdk-android/tree/master)

Punch Through's SDK for speeding up development with the LightBlue Bean development platform. Build Android apps that talk to your Beans.

* [Bean SDK API Documentation](http://punchthrough.github.io/bean-sdk-android/)
* [Bean SDK listing at Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.punchthrough.bean.sdk%22)

# Install

If you're using Android Studio, you can add the Bean SDK to your project by adding a dependency in your Gradle build file.

Your project's Gradle build file is located at `/path/to/YOUR_PROJECT_HERE/app/build.gradle`. Note that this file is inside the `app` folder.

Inside your Gradle build file, add the Bean SDK to the `dependencies` block:

```groovy
dependencies {
    ...
    compile 'com.punchthrough.bean.sdk:sdk:v.v.v'
    ...
}
```

*Note:* Make sure you replace `v.v.v` above with a valid version number!

Also, the Bean SDK has a minimum Android API requirement of `18`. Make sure that this is configured properly by editing the following in the Gradle build file:

```groovy
android {
    ...
    
    defaultConfig {
        ...
        
        minSdkVersion 18
    }
}
```

Then sync with Gradle and Android Studio will install the Bean SDK from Maven Central.

# Usage and Examples

Check out our [Bean SDK API Documentation](http://punchthrough.github.io/bean-sdk-android/) to learn more about the Bean SDK's available methods.

## List nearby Bean(s)

This snippet makes use of the `BeanManager` and the Listener pattern to provide callbacks when Beans are discovered and when the discovery process is complete.

```java
final List<Bean> beans = new ArrayList<>();

BeanDiscoveryListener listener = new BeanDiscoveryListener() {
    @Override
    public void onBeanDiscovered(Bean bean, int rssi) {
        beans.add(bean);
    }

    @Override
    public void onDiscoveryComplete() {
        // This is called when the scan times out, defined by the .setScanTimeout(int seconds) method
    
        for (Bean bean : beans) {
            System.out.println(bean.getDevice().getName());   // "Bean"              (example)
            System.out.println(bean.getDevice().mAddress);    // "B4:99:4C:1E:BC:75" (example)
        }

    }
};

BeanManager.getInstance().setScanTimeout(15);  // Timeout in seconds, optional, default is 30 seconds
BeanManager.getInstance().startDiscovery(listener);

```

## Read device information

The following snippet will connect to a `Bean` and read it's device information using the Device Information Service (DIS) BLE profile.

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

## Blink the on-board LED

This snippet assumes you have a connected `Bean` object, like from the [Read device information second](https://github.com/PunchThrough/bean-sdk-android#read-device-information).

Also, you shouldn't `Thread.sleep()`, this is just to show off the `LedColor` and `Bean` API.

```
LedColor blue = LedColor.create(0, 0, 255);
LedColor off = LedColor.create(0, 0, 0);
int sleep = 500;  // Milliseconds

try {
    bean.setLed(blue);
    Thread.sleep(sleep);
    bean.setLed(off);
    Thread.sleep(sleep);
} catch (InterruptedException e) {
    e.printStackTrace()
}
```

## Upload a Sketch

This example is not for novice users. Our SDK API does not handle the compilation of sketches from `.ino` files into [Intel Hex format](https://en.wikipedia.org/wiki/Intel_HEX). For this, you can make use of Arduino build tools such as [platformio](http://platformio.org/) or [Arduino Builder](https://github.com/arduino/arduino-builder).

This snippet assumes you have a connected `Bean` object, like from the [Read device information second](https://github.com/PunchThrough/bean-sdk-android#read-device-information).

First, we need to create a `SketchHex` object.

```
final String hexPath = "path/to/intel/hex/file"

InputStream fileStream  = getContext().getAssets().open(hexPath);
StringWriter writer = new StringWriter();
IOUtils.copy(fileStream, writer);

// Finally, the SketchHex object
SketchHex sketchHex = SketchHex.create(timestamp, writer.toString());
```

Next, define a callback of type `Callback<UploadProgress>` that will back called back when sketch upload progress has been made.

```
final CountDownLatch sketchLatch = new CountDownLatch(1);
Callback<UploadProgress> onProgress = new Callback<UploadProgress>() {
    @Override
    public void onResult(UploadProgress result) {
        System.out.println("On Result: " + result);
    }
};
```

And a `Runnable` object that will be executed when the Sketch upload has been completed.

```
Runnable onComplete = new Runnable() {
    @Override
    public void run() {
        System.out.println("all done!");
        sketchLatch.countDown();
    }
};
```

Finally, use the `Bean` API to upload the sketch!

```
bean.programWithSketch(sketchHex, onProgress, onComplete);
```

# Developing and Contributing

Check out [our HACKING file](HACKING.md) to read our developer's guide to the SDK.
