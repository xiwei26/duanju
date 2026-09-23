package com.juku.app.data.api

import com.juku.app.data.model.LoginRequest
import com.juku.app.data.model.ChangePasswordRequest
import com.juku.app.data.model.FollowingUpdateRequest
import com.juku.app.data.model.PlaybackControlRequest
import com.juku.app.data.model.PlaybackClientCapabilities
import com.juku.app.data.model.PlaybackPlanRequest
import com.juku.app.data.model.PlaybackProgress
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class JukuApiContractTest {
    private lateinit var server: MockWebServer
    private lateinit var service: JukuApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
                    .asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(JukuApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun dramasUseTheServerLibraryRouteAndDataEnvelope() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"data":[{"id":"hongguo:1","title":"测试剧","thumb":"/api/ui/image?id=1","typeName":"真人剧"}],"total":1}"""
            )
        )

        val response = service.getDramas(null)

        assertEquals("/api/ui/dramas", server.takeRequest().path)
        assertEquals("http://server.test/api/ui/image?id=1", response.data.single().displayCover("http://server.test/"))
        assertEquals("真人剧", response.data.single().displayCategory())
        assertTrue(response.data.single().matchesCategory("真人剧"))
    }

    @Test
    fun loginUsesJsonOnTheAccountRoute() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"ok":true,"account":{"username":"admin","admin":true,"onlineOnly":false,"sources":["hongguo"],"requirePasswordChange":false}}"""
            )
        )

        service.login(LoginRequest("admin", "password123"))

        val request = server.takeRequest()
        assertEquals("/api/ui/account/login", request.path)
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type")?.lowercase())
        assertEquals("""{"username":"admin","password":"password123"}""", request.body.readUtf8())
    }

    @Test
    fun requiredPasswordChangeUsesTheAccountRoute() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ok":true,"account":{"username":"admin","requirePasswordChange":false}}"""))

        service.changePassword(ChangePasswordRequest("initial-password", "updated-password"))

        val request = server.takeRequest()
        assertEquals("/api/ui/account/password", request.path)
        assertEquals(
            """{"password":"initial-password","newPassword":"updated-password"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun playbackPlanUsesPostJsonInsteadOfQueryParameters() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"delivery":"proxy","url":"/media/file.mp4"}"""))

        service.getPlaybackPlan(
            PlaybackPlanRequest(
                session = "session-1",
                episode = 3,
                version = 7,
                client = PlaybackClientCapabilities(mp4 = true, nativeHls = true)
            )
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/ui/playback/plan", request.path)
        assertEquals(
            """{"session":"session-1","episode":3,"start":0.0,"quality":0,"version":7,"mode":"proxy","client":{"mp4":true,"nativeHls":true,"hlsjs":false,"video":["h264"],"audio":["aac","mp3"]}}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun legacyPlaybackRetriesWithANewerStreamVersion() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"player":"legacy","reason":"video_codec_unsupported"}"""))
        server.enqueue(MockResponse().setBody("""{"url":"/api/ui/playback/hls/index.m3u8?session=s","run":2,"duration":42}"""))

        val plan = requestCompatiblePlaybackPlan(
            service,
            PlaybackPlanRequest(session = "session-1", episode = 3, version = 7)
        )

        assertEquals("/api/ui/playback/hls/index.m3u8?session=s", plan.url)
        assertEquals("/api/ui/playback/plan", server.takeRequest().path)
        val fallback = server.takeRequest()
        assertEquals("/api/ui/playback/hls/open", fallback.path)
        assertTrue(fallback.body.readUtf8().contains("\"version\":8"))
    }

    @Test
    fun closePlaybackCanAtomicallySaveFinalProgress() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        service.controlPlayback(
            PlaybackControlRequest(
                session = "session-1",
                action = "close",
                progress = PlaybackProgress(2, 12, 3, 41.0, 42.0, true)
            )
        )

        val request = server.takeRequest()
        assertEquals("/api/ui/playback/control", request.path)
        assertEquals(
            """{"session":"session-1","action":"close","progress":{"run":2,"sequence":12,"episode":3,"position":41.0,"duration":42.0,"completed":true}}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun followingUpdateUsesPostJson() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        service.updateFollowing(FollowingUpdateRequest("hongguo:123", saved = true))

        val request = server.takeRequest()
        assertEquals("/api/ui/following", request.path)
        assertEquals("""{"dramaId":"hongguo:123","saved":true}""", request.body.readUtf8())
    }
}
