# Developer Guide

This file contains instructions that apply to developers only.

## Run integration tests

You can build, install and execute the android integration tests with gradle:

```sh
./gradlew connectedAndroidTest --continue
```

And then take a peek at the results:

```sh
firefox sdk/build/outputs/reports/androidTests/connected/index.html
```

## Run unit tests

None yet :(

## Building javadocs

```sh
./gradlew -q buildDocs
```

Now look at the new Javadocs!

```sh
firefox build/javadoc/index.html
```

## Deploying Docs

The section above (Building javadocs) can be entirely automated, including the deployment to `gh-pages`.

First, `cd` into `docs/` and install the script dependencies:

```bash
npm install
```

Then deploy to `gh-pages`!

```bash
npm run gulp deploy
```
