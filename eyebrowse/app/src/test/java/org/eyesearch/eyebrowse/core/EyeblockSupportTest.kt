/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.WebExtension
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.eyesearch.eyebrowse.helpers.EyebrowseRobolectricTestRunner

@RunWith(EyebrowseRobolectricTestRunner::class)
class EyeblockSupportTest {
    private val engine = mockk<Engine>(relaxed = true)
    private val extension = mockk<WebExtension>(relaxed = true)
    private val url = "resource://android/assets/extensions/ublock_origin/"

    @Test
    fun `installation failure can be retried successfully`() {
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) } answers {
            arg<(Throwable) -> Unit>(3)(IllegalStateException("install failed"))
            mockk(relaxed = true)
        }
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.ERROR, EyeblockSupport.status.value)

        every { extension.isEnabled() } returns true
        every { extension.isAllowedInPrivateBrowsing() } returns true
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) } answers {
            arg<(WebExtension) -> Unit>(2)(extension)
            mockk(relaxed = true)
        }
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.ENABLED, EyeblockSupport.status.value)
    }

    @Test
    fun `latest switch state wins while installation is pending`() {
        val installed = slot<(WebExtension) -> Unit>()
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, capture(installed), any()) } returns mockk(relaxed = true)
        every { extension.isEnabled() } returns false
        every { extension.isAllowedInPrivateBrowsing() } returns true

        EyeblockSupport.setEnabled(engine, true)
        EyeblockSupport.setEnabled(engine, false)
        installed.captured(extension)

        assertEquals(EyeblockSupport.Status.DISABLED, EyeblockSupport.status.value)
        verify(exactly = 0) { engine.enableWebExtension(any(), any(), any(), any()) }
    }

    @Test
    fun `a changed switch choice is retried after pending installation fails`() {
        val failed = slot<(Throwable) -> Unit>()
        every { extension.isEnabled() } returns false
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), capture(failed)) } returns mockk(relaxed = true)

        EyeblockSupport.setEnabled(engine, true)
        EyeblockSupport.setEnabled(engine, false)
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) } answers {
            arg<(WebExtension) -> Unit>(2)(extension)
            mockk(relaxed = true)
        }
        failed.captured(IllegalStateException("install failed"))

        assertEquals(EyeblockSupport.Status.DISABLED, EyeblockSupport.status.value)
        verify(exactly = 0) { engine.setAllowedInPrivateBrowsing(any(), any(), any(), any()) }
    }

    @Test
    fun `foreground retry recovers an installation failure without opening settings`() {
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) } answers {
            arg<(Throwable) -> Unit>(3)(IllegalStateException("install failed"))
            mockk(relaxed = true)
        }
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.ERROR, EyeblockSupport.status.value)

        every { extension.isEnabled() } returns true
        every { extension.isAllowedInPrivateBrowsing() } returns true
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) } answers {
            arg<(WebExtension) -> Unit>(2)(extension)
            mockk(relaxed = true)
        }
        EyeblockSupport.retryIfNeeded(engine, true)
        assertEquals(EyeblockSupport.Status.ENABLED, EyeblockSupport.status.value)
        EyeblockSupport.retryIfNeeded(engine, true)
        verify(exactly = 2) { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) }
    }

    @Test
    fun `enabling during pending disable reconciles private permission`() {
        val disabled = slot<(WebExtension) -> Unit>()
        every { extension.isEnabled() } returns true
        every { extension.isAllowedInPrivateBrowsing() } returns false
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) } answers {
            arg<(WebExtension) -> Unit>(2)(extension)
            mockk(relaxed = true)
        }
        every { engine.disableWebExtension(extension, any(), capture(disabled), any()) } answers { }
        every { engine.setAllowedInPrivateBrowsing(extension, true, any(), any()) } answers {
            every { extension.isAllowedInPrivateBrowsing() } returns true
            arg<(WebExtension) -> Unit>(2)(extension)
        }
        every { engine.enableWebExtension(extension, any(), any(), any()) } answers {
            every { extension.isEnabled() } returns true
            arg<(WebExtension) -> Unit>(2)(extension)
        }

        EyeblockSupport.setEnabled(engine, false)
        EyeblockSupport.setEnabled(engine, true)
        every { extension.isEnabled() } returns false
        disabled.captured(extension)

        assertEquals(EyeblockSupport.Status.ENABLED, EyeblockSupport.status.value)
        verify(exactly = 1) { engine.setAllowedInPrivateBrowsing(extension, true, any(), any()) }
    }

    @Test
    fun `private permission failure is not reported as fully enabled`() {
        every { extension.isEnabled() } returns true
        every { extension.isAllowedInPrivateBrowsing() } returns false
        every { engine.installBuiltInWebExtension(EyeblockSupport.EXTENSION_ID, url, any(), any()) } answers {
            arg<(WebExtension) -> Unit>(2)(extension)
            mockk(relaxed = true)
        }
        every { engine.setAllowedInPrivateBrowsing(extension, true, any(), any()) } answers {
            arg<(Throwable) -> Unit>(3)(IllegalStateException("private permission failed"))
        }

        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.PRIVATE_UNAVAILABLE, EyeblockSupport.status.value)
    }
}
