# Developer Guide

This file contains instructions that apply to developers only.

# Integration Testing

You can build, install and execute the Android integration tests with Gradle:

```sh
./gradlew connectedAndroidTest --continue
```

And then take a peek at the results:

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

# Helpful Gradle Tasks

* `./gradlew tasks` - lists all available gradle tasks w/ short description
