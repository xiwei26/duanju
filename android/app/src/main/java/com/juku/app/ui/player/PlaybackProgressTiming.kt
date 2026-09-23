package com.juku.app.ui.player

internal fun shouldReportPlaybackProgress(nowMs: Long, lastReportMs: Long?): Boolean =
    lastReportMs == null || nowMs - lastReportMs >= 10_000L
