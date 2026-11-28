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
    private val url = EyeblockSupport.EXTENSION_URL

    private fun installed(vararg extensions: WebExtension) {
        every { engine.listInstalledWebExtensions(any(), any()) } answers {
            arg<(List<WebExtension>) -> Unit>(0)(extensions.toList())
        }
    }

    private fun stubDownload() {
        every { extension.id } returns EyeblockSupport.EXTENSION_ID
        every { engine.installWebExtension(url, any(), any(), any()) } answers {
            arg<(WebExtension) -> Unit>(2)(extension)
            mockk(relaxed = true)
        }
    }

    @Test
    fun `fresh disabled state never downloads`() {
        installed()
        EyeblockSupport.setEnabled(engine, false)
        assertEquals(EyeblockSupport.Status.DISABLED, EyeblockSupport.status.value)
        verify(exactly = 0) { engine.installWebExtension(any(), any(), any(), any()) }
    }

    @Test
    fun `installation failure stays off and explicit retry works`() {
        installed()
        every { engine.installWebExtension(url, any(), any(), any()) } answers {
            arg<(Throwable) -> Unit>(3)(IllegalStateException("download failed"))
            mockk(relaxed = true)
        }
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.ERROR, EyeblockSupport.status.value)
        assertEquals(false, EyeblockSupport.status.value.active)
        assertEquals("DOWNLOADING: Engine error: IllegalStateException", EyeblockSupport.failureDetail)
        EyeblockSupport.retryIfNeeded(engine, true)
        verify(exactly = 1) { engine.installWebExtension(url, any(), any(), any()) }

        every { extension.isEnabled() } returns true
        every { extension.isAllowedInPrivateBrowsing() } returns true
        stubDownload()
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.ENABLED, EyeblockSupport.status.value)
        assertEquals(null, EyeblockSupport.failureDetail)
    }

    @Test
    fun `switch stays off during download and latest choice wins`() {
        installed()
        val completed = slot<(WebExtension) -> Unit>()
        every { extension.id } returns EyeblockSupport.EXTENSION_ID
        every { engine.installWebExtension(url, any(), capture(completed), any()) } returns mockk(relaxed = true)
        every { extension.isEnabled() } returns false
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.DOWNLOADING, EyeblockSupport.status.value)
        assertEquals(false, EyeblockSupport.status.value.active)
        EyeblockSupport.setEnabled(engine, false)
        completed.captured(extension)
        assertEquals(EyeblockSupport.Status.DISABLED, EyeblockSupport.status.value)
        verify(exactly = 0) { engine.enableWebExtension(any(), any(), any(), any()) }
    }

    @Test
    fun `existing regular extension is reused without download`() {
        every { extension.id } returns EyeblockSupport.EXTENSION_ID
        every { extension.isEnabled() } returns true
        every { extension.isAllowedInPrivateBrowsing() } returns true
        installed(extension)
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.ENABLED, EyeblockSupport.status.value)
        verify(exactly = 0) { engine.installWebExtension(any(), any(), any(), any()) }
    }

    @Test
    fun `private permission failure is not reported as fully enabled`() {
        installed()
        stubDownload()
        every { extension.isEnabled() } returns true
        every { extension.isAllowedInPrivateBrowsing() } returns false
        every { engine.setAllowedInPrivateBrowsing(extension, true, any(), any()) } answers {
            arg<(Throwable) -> Unit>(3)(IllegalStateException("private permission failed"))
        }
        EyeblockSupport.setEnabled(engine, true)
        assertEquals(EyeblockSupport.Status.PRIVATE_UNAVAILABLE, EyeblockSupport.status.value)
    }

    @Test
    fun `legacy built in is disabled without downloading on upgrade`() {
        every { extension.id } returns EyeblockSupport.EXTENSION_ID
        every { extension.isBuiltIn() } returns true
        every { extension.isEnabled() } returns true
        installed(extension)
        every { engine.disableWebExtension(extension, any(), any(), any()) } answers {
            every { extension.isEnabled() } returns false
            arg<(WebExtension) -> Unit>(2)(extension)
        }
        EyeblockSupport.setEnabled(engine, false)
        assertEquals(EyeblockSupport.Status.DISABLED, EyeblockSupport.status.value)
        verify(exactly = 0) { engine.installWebExtension(any(), any(), any(), any()) }
    }

    @Test
    fun `permission callback only approves the opted in pinned version`() {
        installed()
        val failed = slot<(Throwable) -> Unit>()
        every { engine.installWebExtension(url, any(), any(), capture(failed)) } returns mockk(relaxed = true)
        every { extension.id } returns EyeblockSupport.EXTENSION_ID
        every { extension.getMetadata() } returns mockk {
            every { version } returns EyeblockSupport.EXTENSION_VERSION
        }
        EyeblockSupport.setEnabled(engine, true)
        var approved = false
        assertEquals(true, EyeblockSupport.handleInstallPermission(extension) { approved = it })
        assertEquals(true, approved)
        assertEquals(EyeblockSupport.Status.INSTALLING, EyeblockSupport.status.value)
        failed.captured(IllegalStateException("test cleanup"))
    }
}
