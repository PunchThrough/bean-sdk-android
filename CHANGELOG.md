# 2.0.1

2016-04-25

Release Notes - Bean Android SDK - Version 2.0.1

## Features

* Improve throughput of OAD procedure dramatically

## Bug fixes

* Fix one-off error in FirmwareImage.block() function

# 2.0.0

2016-04-19

Release Notes - Bean Android SDK - Version 2.0.0

## Features

* Expose Bean.readRemoteRssi() to read RSSI at any time during.
* Massive refactoring of how OAD (firmware updates) are implemented to support Bean+.

## Bug Fixes

* BeanListener.disconnect() is fired reliably when you disconnect from a Bean.
* Make sure all services are ready before calling BeanListener.onConnected()

## Tests

* Many new unit and instrumentation tests for OAD process, BatteryProfile, BeanListener, etc.

# 1.0.3

2016-02-10

Release Notes - Bean Android SDK - Version 1.0.3

## Bug Fixes

* onCharacteristicChanged (Scratch) fails with version 1.0.2

## Tests

* Add unit tests for Bean class
* Setup CircleCI to run JUnit tests
* Convert most Instrumentation tests to unit tests

# 1.0.2

2016-01-05

## Bug Fixes

* Fixed an issue causing scratch data reads to come from the wrong scratch bank (thanks @loune)

## Tests
* Added integration tests:
    * Reading and writing scratch characteristics
    * Scanning for Beans
    * Connecting to Beans
    * Reading device information from Beans
* Added helper functions for finding, connecting to, and interacting with Beans during integration tests

## Docs
* Improved documentation for users and contributors
* Moved the SDK from pre-release to release status

## Build Process
* Added automated command-line tests
* Added a Gradle task to build Javadocs

# 1.0.1

2015-04-08

* Fixed an issue preventing the SDK from connecting to Beans with very old firmware

# 1.0.0

2015-04-07

* Initial release
