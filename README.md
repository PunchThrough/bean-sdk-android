# README #

This is an unofficial SDK for the [Punch Through Design Bean][1]. The library allows for communication with the bean and control its various functions.

### Getting started ###

Add the library as a dependency to your project. When using Android Studio and Gradle it's as easy as adding this dependency:


```
#!groovy

compile 'nl.littlerobots.bean:beanlib:0.9.0'
```

## Examples ##

### Scanning for beans ###

Use the `BeanManager` singleton to scan for available beans like this:

```
#!java
// create a listener
BeanDiscoveryListener listener = ...;
// Assuming "this" is an activity or service:
BeanManager.getInstance().startDiscovery(this, listener);

```
When beans are found, the `onBeanDiscovered` will be fired in your listener.

### Connecting and interacting with the bean ###
Once you got hold of a bean, you can connect to it like this:

```
#!java
// create a listener, it's onConnected will be called when the bean is connected
BeanListener myBeanListener = ...;
myBean.connect(this, myBeanListener);

// after the connection is instantiated, briefly flash the led:
myBean.setLed(255, 0, 0);
myBean.setLed(0, 0, 0);

// reading things takes a callback:
myBean.readSketchMetaData(new Callback<SketchMetaData>() {
                    @Override
                    public void onResult(SketchMetaData result) {
                        Log.i(TAG, "Running sketch "+result);
                    }
                });

// when you're done, don't forget to disconnect
myBean.disconnect();
```

## What's missing? ##

* Programming the bean from Android
* Reading battery level

## Contact ##
This library is maintained by [Hugo Visser][2] of [Little Robots][3]. Check out the website, or get in touch through [Google+][2], [Twitter][4] or on the [Bean talk forum][5] (user [hvisser][6])

## License ##

This code is licensed under the MIT license.
```
/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Little Robots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
```

[1]:http://punchthrough.com/bean/
[2]:https://google.com/+hugovisser
[3]:http://littlerobots.nl
[4]:http://twitter.com/botteaap
[5]:http://beantalk.punchthrough.com/
[6]:http://beantalk.punchthrough.com/users/hvisser/