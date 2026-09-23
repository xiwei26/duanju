package com.juku.app.data.repository

import android.content.Context
import com.juku.app.data.api.ApiClient
import com.juku.app.data.api.requestCompatiblePlaybackPlan
import com.juku.app.data.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.system.measureTimeMillis

class JukuRepository(context: Context) {
    private val apiClient = ApiClient(context)

    val serverUrlFlow: Flow<String> = apiClient.serverUrlFlow
    val currentAccount: Flow<Account?> = apiClient.currentAccount

    suspend fun initServerUrl() {
        apiClient.initServerUrl()
        apiClient.initializeViewer()
    }

    suspend fun saveServerUrl(url: String) {
        apiClient.saveServerUrl(url)
        apiClient.initializeViewer()
    }

    suspend fun getDramas(category: String? = null): Result<LibraryResponse> = runCatching {
        val response = apiClient.requireService().getDramas()
        if (category.isNullOrBlank()) response
        else response.copy(data = response.data.filter { it.matchesCategory(category) })
    }

    suspend fun searchDramas(query: String): Result<List<Drama>> = runCatching {
        apiClient.requireService().searchDramas(query.trim()).data
    }

    suspend fun openPlayback(dramaId: String): Result<PlaybackOpenResponse> = runCatching {
        apiClient.requireService().openPlayback(PlaybackOpenRequest(dramaId = dramaId))
    }

    suspend fun getPlaybackPlan(
        session: String,
        episode: Int,
        start: Double,
        version: Long,
        mode: String = "proxy"
    ): Result<PlaybackPlan> = runCatching {
        val service = apiClient.requireService()
        val request = PlaybackPlanRequest(
            session = session,
            episode = episode,
            start = start,
            version = version,
            mode = mode
        )
        requestCompatiblePlaybackPlan(service, request)
    }

    suspend fun reportProgress(
        session: String,
        run: Long,
        sequence: Long,
        episode: Int,
        position: Double,
        duration: Double,
        completed: Boolean
    ): Result<Unit> = runCatching {
        apiClient.requireService().reportProgress(
            PlaybackProgressRequest(
                session = session,
                progress = PlaybackProgress(
                    run = run,
                    sequence = sequence,
                    episode = episode,
                    position = position,
                    duration = duration,
                    completed = completed
                )
            )
        )
    }

    suspend fun closePlayback(session: String, progress: PlaybackProgress? = null): Result<Unit> = runCatching {
        if (session.isNotBlank()) {
            apiClient.requireService().controlPlayback(PlaybackControlRequest(session, "close", progress))
        }
    }

    suspend fun heartbeatPlayback(session: String): Result<Unit> = runCatching {
        apiClient.requireService().controlPlayback(PlaybackControlRequest(session, "heartbeat"))
    }

    suspend fun getFollowing(): Result<FollowingResponse> = runCatching {
        coroutineScope {
            val following = async { apiClient.requireService().getFollowing().data }
            val history = async {
                apiClient.requireService().getHistory().data.map(HistoryItem::toFollowingItem)
            }
            FollowingResponse(following.await(), history.await())
        }
    }

    suspend fun setFollowing(dramaId: String, saved: Boolean): Result<Unit> = runCatching {
        val response = apiClient.requireService().updateFollowing(FollowingUpdateRequest(dramaId, saved))
        check(response.ok) { "追剧状态未保存" }
    }

    suspend fun getDownloadTasks(): Result<DownloadTasksResponse> = runCatching {
        apiClient.requireService().getDownloadTasks()
    }

    suspend fun triggerMerge(dramaId: String): Result<Unit> = runCatching {
        apiClient.requireService().triggerMerge(MergeRequest(dramaIds = listOf(dramaId)))
    }

    suspend fun checkConnection(): Result<ConnectionStatusResponse> = runCatching {
        var response: ViewerResponse? = null
        val latency = measureTimeMillis { response = apiClient.initializeViewer() }
        check(response?.ready == true) { "服务端未返回可用状态" }
        ConnectionStatusResponse(latencyMs = latency)
    }

    suspend fun login(username: String, password: String): Result<Account> = runCatching {
        apiClient.initializeViewer()
        val result = apiClient.requireService().login(LoginRequest(username.trim(), password))
        check(result.ok && result.account != null) { "服务端未返回登录账号" }
        apiClient.initializeViewer().account ?: error("登录状态未生效")
    }

    suspend fun logout(): Result<Unit> = runCatching {
        apiClient.requireService().logout()
        apiClient.initializeViewer()
    }

    suspend fun changePassword(password: String, newPassword: String): Result<Account> = runCatching {
        val result = apiClient.requireService().changePassword(ChangePasswordRequest(password, newPassword))
        check(result.ok) { "密码修改未生效" }
        apiClient.initializeViewer().account ?: error("账号状态未生效，请重新登录")
    }

    fun getViewerId(): String = apiClient.getViewerId()

    fun playbackHeaders(includeIdentity: Boolean = true): Map<String, String> =
        apiClient.playbackHeaders(includeIdentity)
}
