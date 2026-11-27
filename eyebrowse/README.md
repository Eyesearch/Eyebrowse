# Eyebrowse Android app

This module contains the Eyebrowse Android application. The application project lives in `eyebrowse/`. The Gradle product flavor is `eyebrowse`. The inherited Kotlin namespace remains pending a coordinated migration.

- [Eyebrowse repository](https://github.com/Eyesearch/Eyebrowse)
- [Eyebrowse issues](https://github.com/Eyesearch/Eyebrowse/issues)
- [Repository overview](../README.md)

The following build and development notes are adapted from the upstream Mozilla project. Links to Mozilla tools and documentation identify those external resources, not Eyebrowse support channels.

## Build instructions

Install JDK 17, Android SDK 34 and the Android build tools. Clone this repository, then open the `eyebrowse/` project in Android Studio or run:

```sh
git clone https://github.com/Eyesearch/Eyebrowse.git
cd Eyebrowse/eyebrowse
./gradlew assembleEyebrowseDebug
```

For source-only compilation, use `./gradlew :app:compileEyebrowseDebugKotlin`. The `Eyebrowse` task prefix reflects the product flavor. Refer to the root README for CI triggers.

### Build variants

- Debug: `org.eyesearch.eyebrowse.debug`
- Nightly: `org.eyesearch.eyebrowse.nightly`
- Beta: `org.eyesearch.eyebrowse.beta`
- Release: `org.eyesearch.eyebrowse`

The repository currently uses debug signing for the release APK; configure proper signing before public production distribution. No store publishing status is implied.

For the original upstream development and performance instructions, refer to the [upstream documentation](https://github.com/mozilla-mobile/firefox-android/tree/main/fenix). The notes below describe inherited local-development options; they may require additional validation for this fork.

## Local development options

### Building debuggable release variants

The upstream build system supports non-debuggable Nightly, Beta and Release variants. To locally create debuggable builds of those variants, add the following to `<proj-root>/local.properties`:

```sh
debuggable
```

### Setting raptor manifest flag

To set the raptor manifest flag in Nightly, Beta and Release variants, add the following to `<proj-root>/local.properties`:

```sh
raptorEnabled
```

### Auto-publication workflow for application-services and glean
If you're making changes to these projects and want to test them in Eyebrowse, auto-publication workflow is the fastest, most reliable
way to do that.

In `local.properties`, specify a relative path to your local `glean` and/or `application-services` projects. E.g.:
- `autoPublish.glean.dir=../glean`
- `autoPublish.application-services.dir=../application-services`

Once these flags are set, your Eyebrowse builds will include any local modifications present in these projects.

See a [demo of auto-publication workflow in action](https://www.youtube.com/watch?v=qZKlBzVvQGc).

In order to build successfully, you need to check out a commit in the dependency repository that has no breaking changes. The two best ways to do this are:
- Run the `<android-components>/tools/list_compatible_dependency_versions.py` script to output a compatible commit
- Check out the latest commit from main in this repository and the dependency repository. However, this may fail if there were breaking changes added recently to the dependency.

If you're trying to build Eyebrowse with a local ac AND a local GV, you'll have to use another method: see [this doc](https://github.com/mozilla-mobile/fenix/blob/main/docs/substituting-local-ac-and-gv.md).

### Using Nimbus servers during local development
If you're working with the Nimbus experiments platform, by default for local development Eyebrowse configures Nimbus to not use a server.

If you wish to use a Nimbus server during local development, you can add a `https://` or `file://` endpoint to the `local.properties` file.

- `nimbus.remote-settings.url`

Testing experimental branches should be possible without a server.

### Using custom Glean servers during local development
If you wish to use a custom Glean server during local development, you can add a `https://` endpoint to the `local.properties` file.

- `glean.custom.server.url`

### GeckoView
For building with a local checkout of `mozilla-central` see [Substituting Local GeckoView](https://github.com/mozilla-mobile/firefox-android/blob/main/fenix/docs/substituting-local-gv.md)

## License


    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/

[sec issue]: https://bugzilla.mozilla.org/enter_bug.cgi?assigned_to=nobody%40mozilla.org&bug_ignored=0&bug_severity=normal&bug_status=NEW&cf_fx_iteration=---&cf_fx_points=---&component=Security%3A%20Android&contenttypemethod=list&contenttypeselection=text%2Fplain&defined_groups=1&flag_type-4=X&flag_type-607=X&flag_type-791=X&flag_type-800=X&flag_type-803=X&flag_type-936=X&flag_type-937=X&form_name=enter_bug&groups=mobile-core-security&maketemplate=Remember%20values%20as%20bookmarkable%20template&op_sys=Unspecified&priority=--&product=Fenix&rep_platform=Unspecified&target_milestone=---&version=unspecified
