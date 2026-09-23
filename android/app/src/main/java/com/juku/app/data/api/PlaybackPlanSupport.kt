package com.juku.app.data.api

import com.juku.app.data.model.NativePlaybackRequest
import com.juku.app.data.model.PlaybackPlan
import com.juku.app.data.model.PlaybackPlanRequest

internal suspend fun requestCompatiblePlaybackPlan(
    service: JukuApiService,
    request: PlaybackPlanRequest
): PlaybackPlan {
    val plan = service.getPlaybackPlan(request)
    if (plan.player != "legacy") return plan
    return service.openNativePlayback(
        NativePlaybackRequest(
            session = request.session,
            episode = request.episode,
            start = request.start,
            quality = request.quality,
            version = request.version + 1
        )
    )
}
