/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

// If you ever need to force a toolchain rebuild (taskcluster) then edit the following comment.
// FORCE REBUILD 2023-05-12

class EyebrowseDependenciesPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) = Unit
}

object EyebrowseVersions {
    const val osslicenses_plugin = "0.10.4"

    const val falcon = "2.2.0"
    const val fastlane = "2.1.1"

    const val androidx_activity = "1.7.2"
    const val androidx_benchmark = "1.2.3"
    const val androidx_profileinstaller = "1.3.1"
    const val androidx_legacy = "1.0.0"
    const val androidx_lifecycle = "2.6.1"
    const val androidx_navigation = "2.5.3"
    const val androidx_splash_screen = "1.0.1"
    const val androidx_transition = "1.4.1"
    const val androidx_datastore = "1.0.0"
    const val google_accompanist = "0.32.0"

    const val adjust = "4.38.2"
    const val installreferrer = "2.2"

    const val junit = "5.10.2"
    const val mockk = "1.13.10"

    const val google_ads_id_version = "16.0.0"

    const val google_play_review_version = "2.0.1"

    // keep in sync with the versions used in AS.
    const val protobuf = "3.21.10"
    const val protobuf_plugin = "0.9.4"
}

@Suppress("unused")
object EyebrowseDependencies {
    const val tools_benchmarkgradle = "androidx.benchmark:benchmark-gradle-plugin:${EyebrowseVersions.androidx_benchmark}"

    const val osslicenses_plugin = "com.google.android.gms:oss-licenses-plugin:${EyebrowseVersions.osslicenses_plugin}"

    const val androidx_benchmark_junit4 = "androidx.benchmark:benchmark-junit4:${EyebrowseVersions.androidx_benchmark}"
    const val androidx_benchmark_macro_junit4 = "androidx.benchmark:benchmark-macro-junit4:${EyebrowseVersions.androidx_benchmark}"
    const val androidx_core_splashscreen = "androidx.core:core-splashscreen:${EyebrowseVersions.androidx_splash_screen}"
    const val androidx_profileinstaller = "androidx.profileinstaller:profileinstaller:${EyebrowseVersions.androidx_profileinstaller}"
    const val androidx_activity_ktx = "androidx.activity:activity-ktx:${EyebrowseVersions.androidx_activity}"
    const val androidx_legacy = "androidx.legacy:legacy-support-v4:${EyebrowseVersions.androidx_legacy}"
    const val androidx_lifecycle_common = "androidx.lifecycle:lifecycle-common:${EyebrowseVersions.androidx_lifecycle}"
    const val androidx_safeargs = "androidx.navigation:navigation-safe-args-gradle-plugin:${EyebrowseVersions.androidx_navigation}"
    const val androidx_navigation_fragment = "androidx.navigation:navigation-fragment-ktx:${EyebrowseVersions.androidx_navigation}"
    const val androidx_navigation_ui = "androidx.navigation:navigation-ui:${EyebrowseVersions.androidx_navigation}"
    const val androidx_transition = "androidx.transition:transition:${EyebrowseVersions.androidx_transition}"
    const val androidx_datastore = "androidx.datastore:datastore:${EyebrowseVersions.androidx_datastore}"

    const val google_accompanist_drawablepainter = "com.google.accompanist:accompanist-drawablepainter:${EyebrowseVersions.google_accompanist}"

    const val protobuf_javalite = "com.google.protobuf:protobuf-javalite:${EyebrowseVersions.protobuf}"
    const val protobuf_compiler = "com.google.protobuf:protoc:${EyebrowseVersions.protobuf}"

    const val adjust = "com.adjust.sdk:adjust-android:${EyebrowseVersions.adjust}"
    const val installreferrer = "com.android.installreferrer:installreferrer:${EyebrowseVersions.installreferrer}"

    const val mockk = "io.mockk:mockk:${EyebrowseVersions.mockk}"
    const val mockk_android = "io.mockk:mockk-android:${EyebrowseVersions.mockk}"
    const val falcon = "com.jraska:falcon:${EyebrowseVersions.falcon}"
    const val fastlane = "tools.fastlane:screengrab:${EyebrowseVersions.fastlane}"

    // --- START AndroidX test dependencies --- //
    // N.B.: the versions of these dependencies appear to be pinned together. To avoid bugs, they
    // should always be updated together based on the latest version from the Android test releases page:
    //   https://developer.android.com/jetpack/androidx/releases/test
    // For the full IDs of these test dependencies, see:
    //   https://developer.android.com/training/testing/set-up-project#android-test-dependencies
    private const val androidx_test_orchestrator = "1.4.2"
    private const val androidx_espresso_version = "3.5.1"
    const val espresso_contrib = "androidx.test.espresso:espresso-contrib:$androidx_espresso_version"
    const val espresso_idling_resources = "androidx.test.espresso:espresso-idling-resource:$androidx_espresso_version"
    const val espresso_intents = "androidx.test.espresso:espresso-intents:$androidx_espresso_version"
    // Monitor is unused
    const val orchestrator = "androidx.test:orchestrator:$androidx_test_orchestrator"
    // Truth is unused
    // Test services is unused
    // --- END AndroidX test dependencies --- //

    const val google_ads_id = "com.google.android.gms:play-services-ads-identifier:${EyebrowseVersions.google_ads_id_version}"

    // Required for in-app reviews
    const val google_play_review = "com.google.android.play:review:${EyebrowseVersions.google_play_review_version}"
    const val google_play_review_ktx = "com.google.android.play:review-ktx:${EyebrowseVersions.google_play_review_version}"

    const val junitApi = "org.junit.jupiter:junit-jupiter-api:${EyebrowseVersions.junit}"
    const val junitParams = "org.junit.jupiter:junit-jupiter-params:${EyebrowseVersions.junit}"
    const val junitEngine = "org.junit.jupiter:junit-jupiter-engine:${EyebrowseVersions.junit}"
}

/**
 * Functionality to limit specific dependencies to specific repositories. These are typically expected to be used by
 * dependency group name (i.e. with `include/excludeGroup`). For additional info, see:
 * https://docs.gradle.org/current/userguide/declaring_repositories.html#sec::matching_repositories_to_dependencies
 *
 * Note: I wanted to nest this in Deps but for some reason gradle can't find it so it's top-level now. :|
 */
object RepoMatching {
    const val mozilla = "org\\.mozilla\\..*"
    const val androidx = "androidx\\..*"
    const val comAndroid = "com\\.android.*"
    const val comGoogle = "com\\.google\\..*"
}
