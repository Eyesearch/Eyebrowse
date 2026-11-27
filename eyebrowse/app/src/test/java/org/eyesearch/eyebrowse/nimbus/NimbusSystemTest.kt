/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.nimbus

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import mozilla.components.service.nimbus.messaging.NimbusSystem
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mozilla.experiments.nimbus.NimbusInterface
import org.eyesearch.eyebrowse.experiments.maybeFetchExperiments
import org.eyesearch.eyebrowse.ext.settings
import org.eyesearch.eyebrowse.utils.Settings

class NimbusSystemTest {

    lateinit var context: Context
    lateinit var nimbus: NimbusUnderTest
    lateinit var settings: Settings

    private val lastTimeSlot = slot<Long>()

    // By default this comes from the generated Nimbus features.
    val config = NimbusSystem(
        refreshIntervalForeground = 60, // minutes
    )

    class NimbusUnderTest(override val context: Context) : NimbusInterface {
        var isFetching = false

        override fun fetchExperiments() {
            isFetching = true
        }
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        nimbus = NimbusUnderTest(context)

        settings = mockk(relaxed = true)
        every { context.settings() } returns settings

        every { settings.nimbusLastFetchTime = capture(lastTimeSlot) } just runs
        every { settings.nimbusLastFetchTime } returns 0L

        assertFalse(nimbus.isFetching)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments after an interval THEN do not fetch while experiments are disabled`() {
        val elapsedTime: Long = Settings.ONE_HOUR_MS + 1
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime,
        )
        assertFalse(nimbus.isFetching)
        assertFalse(lastTimeSlot.isCaptured)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments at exactly an interval THEN do not fetch while experiments are disabled`() {
        val elapsedTime: Long = Settings.ONE_HOUR_MS
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime,
        )
        assertFalse(nimbus.isFetching)
        assertFalse(lastTimeSlot.isCaptured)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments before an interval THEN do not call fetchExperiments`() {
        val elapsedTime: Long = Settings.ONE_HOUR_MS - 1
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime,
        )
        assertFalse(nimbus.isFetching)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments at without an elapsedTime THEN do not fetch while experiments are disabled`() {
        // since elapsedTime = currentTimeMillis
        nimbus.maybeFetchExperiments(
            context,
            config,
        )
        assertFalse(nimbus.isFetching)
    }

    @Test
    fun `GIVEN a nimbus object calling maybeFetchExperiments WHEN using a preview collection THEN do not fetch while experiments are disabled`() {
        var currentTime = 0L
        fun assertNoFetch() {
            nimbus.maybeFetchExperiments(
                context,
                config,
                currentTime,
            )
            assertFalse(nimbus.isFetching)
            assertFalse(lastTimeSlot.isCaptured)
            nimbus.isFetching = false
        }

        // Preview mode must not bypass the disabled-experiments policy.
        every { settings.nimbusUsePreview } returns true
        currentTime = Settings.ONE_HOUR_MS
        assertNoFetch()

        currentTime += Settings.ONE_MINUTE_MS
        assertNoFetch()

        currentTime += Settings.ONE_MINUTE_MS
        assertNoFetch()

        // Now turn preview collection off.
        // Disabling preview must not enable remote fetching.
        every { settings.nimbusUsePreview } returns false

        currentTime += Settings.ONE_MINUTE_MS
        nimbus.maybeFetchExperiments(
            context,
            config,
            currentTime,
        )
        assertFalse(nimbus.isFetching)
        assertFalse(lastTimeSlot.isCaptured)
        nimbus.isFetching = false
        every { settings.nimbusLastFetchTime } returns currentTime

        // Repeated calls remain disabled.
        currentTime += Settings.ONE_MINUTE_MS
        nimbus.maybeFetchExperiments(
            context,
            config,
            currentTime,
        )
        assertFalse(nimbus.isFetching)

        // Even after an hour, experiments remain disabled.
        currentTime += Settings.ONE_HOUR_MS + Settings.ONE_MINUTE_MS
        nimbus.maybeFetchExperiments(
            context,
            config,
            currentTime,
        )
        assertFalse(nimbus.isFetching)
        assertFalse(lastTimeSlot.isCaptured)
    }
}
