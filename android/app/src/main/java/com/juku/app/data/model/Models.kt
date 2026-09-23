package com.juku.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class Drama(
    val id: String,
    val source: String = "",
    val title: String = "",
    val name: String = "",
    val desc: String = "",
    val intro: String = "",
    val heat: String? = null,
    val category: String = "",
    val categoryName: String = "",
    val category_name: String = "",
    val cover: JsonElement? = null,
    val coverUrl: JsonElement? = null,
    val cover_url: JsonElement? = null,
    val image: JsonElement? = null,
    val imageUrl: JsonElement? = null,
    val image_url: JsonElement? = null,
    val img: JsonElement? = null,
    val pic: JsonElement? = null,
    val picture: JsonElement? = null,
    val poster: JsonElement? = null,
    val thumb: JsonElement? = null,
    val thumbnail: JsonElement? = null,
    val totalEpisode: JsonElement? = null,
    val total_episode: JsonElement? = null,
    val chapterCount: JsonElement? = null,
    val chapter_count: JsonElement? = null,
    val episodeCount: JsonElement? = null,
    val episode_count: JsonElement? = null,
    val total: JsonElement? = null,
    val episodes: JsonElement? = null,
    val typeName: String = "",
    val type_name: String = "",
    val sortName: String = "",
    val sort_name: String = ""
) {
    fun displayCover(serverUrl: String): String {
        val value = listOf(cover, coverUrl, cover_url, image, imageUrl, image_url, img, pic, picture, poster, thumb, thumbnail)
            .firstNotNullOfOrNull(::jsonText)
            .orEmpty()
        return if (value.startsWith("/")) serverUrl.trimEnd('/') + value else value
    }

    fun displayTitle(): String = title.ifBlank { name.ifBlank { "短剧" } }

    fun displayCategory(): String = listOf(categoryName, category_name, category, typeName, type_name, sortName, sort_name)
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

    fun displayEpisodeCount(): String {
        val count = listOf(totalEpisode, total_episode, chapterCount, chapter_count, episodeCount, episode_count, total, episodes)
            .firstNotNullOfOrNull(::jsonPositiveInt)
        return count?.let { "共${it}集" } ?: "集数未知"
    }

    fun matchesCategory(selected: String): Boolean {
        if (selected.isBlank() || selected == "全部") return true
        return listOf(categoryName, category_name, category, typeName, type_name, sortName, sort_name)
            .any { it == selected }
    }
}

private fun jsonText(value: JsonElement?): String? =
    (value as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

private fun jsonPositiveInt(value: JsonElement?): Int? {
    val primitive = value as? JsonPrimitive ?: return null
    return (primitive.intOrNull ?: primitive.contentOrNull?.trim()?.toIntOrNull())?.takeIf { it > 0 }
}

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val index: Int = 0,
    val episode: String = "",
    val number: Int = 0,
    val total: Int = 0,
    val danmaku: Boolean = false
) {
    fun displayTitle(): String = title.ifBlank { "第 ${number.takeIf { it > 0 } ?: index} 集" }
}

@Serializable
data class ViewerResponse(
    val ready: Boolean = false,
    val id: String = "",
    val onlineOnly: Boolean = false,
    val sources: List<String> = emptyList(),
    val account: Account? = null,
    val requireLogin: Boolean = false,
    val allowRegistration: Boolean = true
)

@Serializable
data class Account(
    val username: String,
    val admin: Boolean = false,
    val onlineOnly: Boolean = false,
    val sources: List<String> = emptyList(),
    val requirePasswordChange: Boolean = false
)

typealias AccountPublic = Account

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class ChangePasswordRequest(val password: String, val newPassword: String)

@Serializable
data class AccountOperationResponse(val ok: Boolean = false, val account: Account? = null)

@Serializable
class EmptyRequest

@Serializable
data class LibraryResponse(
    val data: List<Drama> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = false,
    val error: String = ""
)

@Serializable
data class SearchResponse(
    val data: List<Drama> = emptyList(),
    val total: Int = 0,
    val warning: String = ""
)

@Serializable
data class PlaybackOpenRequest(
    val dramaId: String,
    val resume: Boolean = true,
    val fromHistory: Boolean = false
)

@Serializable
data class PlaybackEpisode(
    val index: Int,
    val episode: String = "",
    val title: String = "",
    val chapterId: String = "",
    val number: Int = 0,
    val total: Int = 0,
    val danmaku: Boolean = false
) {
    fun toChapter(): Chapter = Chapter(
        id = chapterId,
        title = title,
        index = index,
        episode = episode,
        number = number,
        total = total,
        danmaku = danmaku
    )
}

@Serializable
data class PlaybackOpenResponse(
    val session: String,
    val dramaId: String,
    val title: String,
    val source: String = "",
    val episodes: List<PlaybackEpisode> = emptyList(),
    val initialIndex: Int = 1,
    val initialPosition: Double = 0.0,
    val resumePaused: Boolean = false
)

@Serializable
data class PlaybackClientCapabilities(
    val mp4: Boolean = true,
    val nativeHls: Boolean = true,
    val hlsjs: Boolean = false,
    val video: List<String> = listOf("h264"),
    val audio: List<String> = listOf("aac", "mp3")
)

@Serializable
data class PlaybackPlanRequest(
    val session: String,
    val episode: Int,
    val start: Double = 0.0,
    val quality: Int = 0,
    val version: Long,
    val mode: String = "proxy",
    val client: PlaybackClientCapabilities = PlaybackClientCapabilities()
)

@Serializable
data class NativePlaybackRequest(
    val session: String,
    val episode: Int,
    val start: Double = 0.0,
    val quality: Int = 0,
    val version: Long
)

@Serializable
data class PlaybackPlan(
    val delivery: String = "proxy",
    val url: String = "",
    val directURL: String? = null,
    val run: Long = 0,
    val player: String = "",
    val mime: String = "",
    val duration: Double = 0.0,
    val quality: Int = 0,
    val reason: String = ""
) {
    fun resolvedUrl(serverUrl: String): String {
        val selected = if (delivery == "redirect" && !directURL.isNullOrBlank()) directURL else url
        return if (selected.startsWith("/")) serverUrl.trimEnd('/') + selected else selected
    }
}

@Serializable
data class PlaybackProgress(
    val run: Long,
    val sequence: Long,
    val episode: Int,
    val position: Double,
    val duration: Double,
    val completed: Boolean
)

@Serializable
data class PlaybackProgressRequest(val session: String, val progress: PlaybackProgress)

@Serializable
data class PlaybackControlRequest(
    val session: String,
    val action: String,
    val progress: PlaybackProgress? = null
)

@Serializable
data class FollowingItem(
    val dramaId: String,
    val title: String,
    val category: String = "",
    val saved: Boolean = false,
    val completed: Boolean = false,
    val knownEpisodes: Int = 0,
    val totalEpisode: Int = 0,
    val newEpisodes: Int = 0,
    val episode: String? = null,
    val coverUrl: String = "",
    val position: Double = 0.0,
    val duration: Double = 0.0
)

@Serializable
data class FollowingUpdateRequest(val dramaId: String, val saved: Boolean)

@Serializable
data class OperationResponse(val ok: Boolean = false)

@Serializable
data class HistoryItem(
    val dramaId: String,
    val title: String,
    val episode: String = "",
    val index: Int = 0,
    val total: Int = 0,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val completed: Boolean = false
) {
    fun toFollowingItem(): FollowingItem = FollowingItem(
        dramaId = dramaId,
        title = title,
        completed = completed,
        knownEpisodes = total,
        totalEpisode = total,
        episode = episode,
        position = position,
        duration = duration
    )
}

data class FollowingResponse(val following: List<FollowingItem>, val history: List<FollowingItem>)

@Serializable
data class DataResponse<T>(val data: T)

@Serializable
data class DownloadTask(
    val id: String,
    val dramaId: String,
    val dramaTitle: String,
    val title: String = "",
    val episode: String = "",
    val index: Int = 0,
    val total: Int = 0,
    val status: String,
    val progress: Int = 0,
    val speedBytesPerSecond: Double = 0.0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String = ""
)

@Serializable
data class DownloadTasksResponse(val data: List<DownloadTask> = emptyList())

@Serializable
data class MergeRequest(
    val dramaIds: List<String>,
    val deleteEpisodes: Boolean = false,
    val async: Boolean = true
)

data class ConnectionStatusResponse(val latencyMs: Long, val status: String = "ok")

@Serializable
data class DanmakuItem(val time: Double, val text: String, val color: String = "#FFFFFF")
