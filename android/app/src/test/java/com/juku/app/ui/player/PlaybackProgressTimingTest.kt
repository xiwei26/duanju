package com.juku.app.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressTimingTest {
    @Test
    fun reportsAfterTenSecondsEvenWhenViewerSeeksBackwards() {
        assertFalse(shouldReportPlaybackProgress(nowMs = 9_999, lastReportMs = 0))
        assertTrue(shouldReportPlaybackProgress(nowMs = 10_000, lastReportMs = 0))
        assertTrue(shouldReportPlaybackProgress(nowMs = 20_001, lastReportMs = 10_000))
    }

    @Test
    fun firstProgressOfEachEpisodeIsReportedImmediately() {
        assertTrue(shouldReportPlaybackProgress(nowMs = 7, lastReportMs = null))
    }
}
