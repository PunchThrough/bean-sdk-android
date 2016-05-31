# 2.1.0

2016-05-31

Release Notes - Bean Android SDK - Version 2.1.0

## Features

* Public Bean API for reading HW/FW versions individually (instead of .readDeviceInfo())
* Add Android permissions ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION
* Implement deterministic scan duration with customizable scan timeout
* New interface for communicating to/from the OADProfile for FW updates
* Generic watchdog implementation, currently only used by OAD process

## Tests

* Support for `beanAddress` as a gradle property for test purposes which allows you to hardcode a BLE address used for instrumentation tests.
* Added "pure Android" BLE test, used for debugging purposes only, which eliminates the use of the SDK itself
* Support in `TestBeanFirmwareUpdate` for Bean+
* Added instrumentation test for `BeanManager`
* Added instrumentation tests for new Bean API (`readHardwareVersion`, `readFirmwareVersion`)

# 2.0.1

2016-04-25

Release Notes - Bean Android SDK - Version 2.0.1

## Features

* Turbo OAD implementation (up to 8 FW blocks "in air", observed ~3.5 Kb/s)

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
