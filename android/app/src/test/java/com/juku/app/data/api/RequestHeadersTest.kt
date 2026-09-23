package com.juku.app.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RequestHeadersTest {
    @Test
    fun serverMediaReceivesViewerAndCookie() {
        val headers = buildRequestHeaders(
            viewerId = "viewer-123",
            cookieHeader = "juku_viewer=guest; juku_account=session",
            includeIdentity = true
        )

        assertEquals("viewer-123", headers["X-Juku-Viewer"])
        assertEquals("juku_viewer=guest; juku_account=session", headers["Cookie"])
        assertEquals("same-origin", headers["Sec-Fetch-Site"])
    }

    @Test
    fun directUpstreamMediaNeverReceivesServerIdentity() {
        val headers = buildRequestHeaders(
            viewerId = "viewer-123",
            cookieHeader = "juku_account=session",
            includeIdentity = false
        )

        assertFalse(headers.containsKey("X-Juku-Viewer"))
        assertFalse(headers.containsKey("Cookie"))
        assertFalse(headers.containsKey("Sec-Fetch-Site"))
        assertEquals("JukuApp/1.0 Android", headers["User-Agent"])
    }
}
