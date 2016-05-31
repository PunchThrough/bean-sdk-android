# Developer Guide

This file contains instructions that apply to developers only.

# Automated Testing

## Unit Tests

Unit tests can be run on your development PC and __do not__ require an Android device or emulator. Run them frequently!

```sh
./gradlew clean assembleDebug assembleRelease test
```

## Instrumentation Tests

These tests are designed to run on an Android device or emulator. You can build, install and execute the Android integration tests with the `connectedAndroidTest` Gradle command.  You need a nearby Bean that the Android device can connect to.  

### Run the tests

Example: Run the tests against a Bean with **the strongest RSSI**.

```sh
./gradlew connectedAndroidTest -i
```

Example: Run the tests against a Bean with a specific **name**.`


```sh
./gradlew -PbeanName=\"TESTBEAN\" connectedAndroidTest -i
```

Example: Run the tests against a Bean with a specific **address**.

```sh
./gradlew -PbeanAddress=\"C4:BE:84:49:BD:3C\" connectedAndroidTest -i
```

Note: The `beanName` and `beanAddress` variables are Gradle properties, so they can be set in a .properties file, command-line argument, or an environment variable.

### Look at results

```sh
firefox sdk/build/outputs/reports/androidTests/connected/index.html
```

# Build and Release

This project is built using [Gradle](http://gradle.org/) and hosted on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.punchthrough.bean.sdk%22).

## Build Process

Here is a high-level description of the build process.

1. Compile code
2. Generate (build) artifacts
3. Digitally sign the artifacts
4. Upload __staging__ artifacts to [Sonatype Nexus](https://oss.sonatype.org/#welcome)
5. Promote staging artifacts to production

Start by configuring your build environment (see **Build Environment Configuration** below). Once that's done, you can automate steps 1-4 by running a single command:

```bash
./gradlew uploadArchives
```

If that command completes without error, you can view the _Staging Repository_ that was just published by the build script on the [Nexus Dashboard](https://oss.sonatype.org/#stagingRepositories). Find the repository that was just published in the list and click "Close" which will automatically promote it as an official release!

## Build Environment Configuration

Create a configuration file at `~/.gradle/gradle.properties` and fill it with the following:

```
systemProp.nexusUsername=punchthrough
systemProp.nexusPassword=PUNCH_THROUGH_NEXUS_PASSWORD
signing.keyId=YOUR_RSA_KEY_ID
signing.password=YOUR_RSA_KEY_PASSPHRASE
signing.secretKeyRingFile=/Users/YOUR_USERNAME/.gnupg/secring.gpg
```

Replace the placeholder values with the appropriate information. This information can be given to you by a project administrator ([Stephen Stack](http://github.com/swstack) and [Matthew Lewis](http://github.com/mplewis)).

# Javadocs

## Building

```sh
./gradlew -q buildDocs
```

Now look at the new Javadocs!

```sh
firefox build/javadoc/index.html
```

## Deploying to GitHub Pages

Building and deploying Javadocs to GitHub Pages is an automated process.

First, `cd` into `docs/` and install the script dependencies:

```bash
npm install
```

Then deploy to `gh-pages`!

```bash
npm run gulp deploy
```

# SDK Local Dependency

This section will tell you how to use the SDK as a local dependency in another project. This is useful during development when you want to test out SDK changes __in a different application__ before a new version of the SDK is released to Maven Central.

This process has two steps...

1. Setup your application to consume the SDK as a local dependency in the form of a `.jar` file.
2. Build the SDK as a `.jar` file and put it somewhere your application can find it.

__Step 1__

Edit your applications `app/build.gradle` file so that it contains the following line in the `dependecies{}` block:

```groovy
dependencies {
    ...
    
    compile fileTree(dir: 'libs', include: ['*.jar'])
}
```

This tells Gradle to compile all `.jar` files in the folder `app/libs`. Make sure this folder exists! To be clear, this step is __not__ talking about the SDK project, we are referring to a different project (application) in which you want to include the SDK as a local dependency.

__Step 2__

Run the `export_jars.py` script in __the SDK project__. It takes one argument, which is the path to your applications `app/libs` folder from step 1.

Example:

```bash
./export_jars.py /my/android/project/app/libs
```

# Helpful Gradle Tasks

* `./gradlew tasks` - lists all available gradle tasks w/ short description
* `./gradlew -q help --task <task>` - detailed information about a specific task
