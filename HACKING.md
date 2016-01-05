# Developer Guide

This file contains instructions that apply to developers only.

# List Tasks

Gradle will list all tasks with a brief description of what they do:

```sh
./gradlew tasks
```

# Integration Testing


You can build, install and execute the Android integration tests with Gradle:

```sh
./gradlew connectedAndroidTest --continue
```

And then take a peek at the results:

```sh
firefox sdk/build/outputs/reports/androidTests/connected/index.html
```

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
