package com.juku.app.data.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.juku.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "juku_prefs")

internal fun buildRequestHeaders(
    viewerId: String,
    cookieHeader: String,
    includeIdentity: Boolean
): Map<String, String> = buildMap {
    put("User-Agent", "JukuApp/1.0 Android")
    if (includeIdentity) {
        put("Sec-Fetch-Site", "same-origin")
        viewerId.takeIf { it.isNotBlank() }?.let { put("X-Juku-Viewer", it) }
        cookieHeader.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
    }
}

interface JukuApiService {
    @GET("api/ui/viewer")
    suspend fun getViewer(@Query("confirm") confirm: Int? = null): ViewerResponse

    @GET("api/ui/dramas")
    suspend fun getDramas(@Query("category") category: String? = null): LibraryResponse

    @GET("api/ui/search")
    suspend fun searchDramas(@Query("q") query: String): SearchResponse

    @POST("api/ui/playback/open")
    suspend fun openPlayback(@Body request: PlaybackOpenRequest): PlaybackOpenResponse

    @POST("api/ui/playback/plan")
    suspend fun getPlaybackPlan(@Body request: PlaybackPlanRequest): PlaybackPlan

    @POST("api/ui/playback/hls/open")
    suspend fun openNativePlayback(@Body request: NativePlaybackRequest): PlaybackPlan

    @POST("api/ui/playback/progress")
    suspend fun reportProgress(@Body request: PlaybackProgressRequest)

    @POST("api/ui/playback/control")
    suspend fun controlPlayback(@Body request: PlaybackControlRequest)

    @GET("api/ui/following")
    suspend fun getFollowing(): DataResponse<List<FollowingItem>>

    @POST("api/ui/following")
    suspend fun updateFollowing(@Body request: FollowingUpdateRequest): OperationResponse

    @GET("api/ui/playback/history")
    suspend fun getHistory(): DataResponse<List<HistoryItem>>

    @GET("api/ui/tasks")
    suspend fun getDownloadTasks(): DownloadTasksResponse

    @POST("api/ui/merge")
    suspend fun triggerMerge(@Body request: MergeRequest)

    @POST("api/ui/account/login")
    suspend fun login(@Body request: LoginRequest): AccountOperationResponse

    @POST("api/ui/account/logout")
    suspend fun logout(@Body request: EmptyRequest = EmptyRequest())

    @POST("api/ui/account/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): AccountOperationResponse
}

class ApiClient(private val context: Context) {
    companion object {
        private const val DEFAULT_SERVER_URL = "http://10.0.2.2:8999/"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val cookieJar = PersistentCookieJar(context)
    private val accountState = MutableStateFlow<Account?>(null)
    private val serverUrlKey = stringPreferencesKey("server_url")

    @Volatile
    private var viewerId = ""

    @Volatile
    private var activeServerUrl = DEFAULT_SERVER_URL

    @Volatile
    private var serviceGeneration = 0L

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "JukuApp/1.0 Android")
                .header("Sec-Fetch-Site", "same-origin")
            viewerId.takeIf { it.isNotBlank() }?.let { request.header("X-Juku-Viewer", it) }
            chain.proceed(request.build())
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    var apiService: JukuApiService? = null
        private set

    val serverUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[serverUrlKey] ?: DEFAULT_SERVER_URL }

    val currentAccount: Flow<Account?> = accountState.asStateFlow()

    suspend fun initServerUrl() {
        updateBaseUrl(serverUrlFlow.first())
    }

    suspend fun saveServerUrl(url: String) {
        val normalized = normalizeServerUrl(url)
        context.dataStore.edit { preferences -> preferences[serverUrlKey] = normalized }
        updateBaseUrl(normalized)
    }

    fun requireService(): JukuApiService =
        apiService ?: error("请先在设置中配置并连接果果剧库服务端")

    suspend fun initializeViewer(): ViewerResponse {
        val service = requireService()
        val generation = serviceGeneration
        var response = service.getViewer()
        if (!response.ready) response = service.getViewer(confirm = 1)
        check(response.ready && response.id.matches(Regex("^[a-f0-9]{64}$"))) {
            "服务端未能建立客户端身份，请检查 Cookie 与服务端版本"
        }
        check(generation == serviceGeneration && service === apiService) {
            "服务地址已更改，请重试"
        }
        viewerId = response.id
        accountState.value = response.account
        return response
    }

    fun getViewerId(): String = viewerId

    fun playbackHeaders(includeIdentity: Boolean = true): Map<String, String> {
        val url = activeServerUrl.toHttpUrl()
        val cookies = cookieJar.loadForRequest(url)
        return buildRequestHeaders(
            viewerId = viewerId,
            cookieHeader = cookies.joinToString("; ") { "${it.name}=${it.value}" },
            includeIdentity = includeIdentity
        )
    }

    private fun updateBaseUrl(url: String) {
        val normalized = normalizeServerUrl(url)
        activeServerUrl = normalized
        serviceGeneration += 1
        viewerId = ""
        accountState.value = null
        apiService = Retrofit.Builder()
            .baseUrl(normalized)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JukuApiService::class.java)
    }

    private fun normalizeServerUrl(url: String): String {
        val normalized = url.trim().trimEnd('/') + "/"
        val parsed = normalized.toHttpUrl()
        require(parsed.scheme == "http" || parsed.scheme == "https") { "服务地址必须使用 HTTP 或 HTTPS" }
        return parsed.toString()
    }
}
