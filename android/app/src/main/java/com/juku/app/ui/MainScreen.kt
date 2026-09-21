package com.juku.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.juku.app.data.model.*
import com.juku.app.data.repository.JukuRepository
import com.juku.app.ui.components.JukuBottomNavBar
import com.juku.app.ui.download.DownloadScreen
import com.juku.app.ui.following.FollowingScreen
import com.juku.app.ui.home.HomeScreen
import com.juku.app.ui.player.PlayerScreen
import com.juku.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(repository: JukuRepository) {
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val serverUrl by repository.serverUrlFlow.collectAsState(initial = "http://192.168.1.108:8999/")
    val currentAccount by repository.currentAccount.collectAsState()

    var dramas by remember { mutableStateOf<List<Drama>>(emptyList()) }
    var isLoadingDramas by remember { mutableStateOf(false) }

    var followingList by remember { mutableStateOf<List<FollowingItem>>(emptyList()) }
    var historyList by remember { mutableStateOf<List<FollowingItem>>(emptyList()) }
    var downloadTasks by remember { mutableStateOf<List<DownloadTask>>(emptyList()) }
    var connectionStatus by remember { mutableStateOf<String?>("已连接 · Go 服务端运行中") }

    // Active Player State
    var activeDrama by remember { mutableStateOf<Drama?>(null) }
    var activeChapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var activeEpisodeIndex by remember { mutableIntStateOf(0) }
    var activeVideoUrl by remember { mutableStateOf("") }
    var danmakuList by remember { mutableStateOf<List<DanmakuItem>>(emptyList()) }

    // Fetch initial dramas
    fun loadDramas(category: String? = null) {
        coroutineScope.launch {
            isLoadingDramas = true
            repository.getDramas(category).onSuccess { list ->
                dramas = list
            }.onFailure {
                // If backend is unreachable or empty, provide graceful fallback demo items
                if (dramas.isEmpty()) {
                    dramas = listOf(
                        Drama(
                            id = "hongguo:7670772535931178008",
                            title = "普通弓箭手 第五季",
                            desc = "逆鳞受损，少年手持灵弦反杀，踏破十万反派包围圈！",
                            heat = "98.5万",
                            categoryName = "逆袭·都市"
                        ),
                        Drama(
                            id = "hongguo:7670772535931178009",
                            title = "千亿龙婿王者归来",
                            desc = "隐忍三年，战神龙婿一朝亮明身份，威震全球！",
                            heat = "89.4万",
                            categoryName = "战神·赘婿"
                        ),
                        Drama(
                            id = "hongguo:7670772535931178010",
                            title = "重回1990当首富",
                            desc = "携三十年记忆重生，在改革春风中打造千亿商业帝国！",
                            heat = "76.1万",
                            categoryName = "商战·穿越"
                        ),
                        Drama(
                            id = "hongguo:7670772535931178011",
                            title = "我在异界当领主",
                            desc = "开局一座破城堡，看我如何建立不朽魔导帝国！",
                            heat = "72.8万",
                            categoryName = "玄幻·异界"
                        )
                    )
                }
            }
            isLoadingDramas = false
        }
    }

    // Play Drama
    fun openDrama(drama: Drama, episodeIndex: Int = 0) {
        coroutineScope.launch {
            activeDrama = drama
            activeEpisodeIndex = episodeIndex

            repository.getDramaDetail(drama.id).onSuccess { detail ->
                activeChapters = detail.chapters
                val chapter = detail.chapters.getOrNull(episodeIndex)
                if (chapter != null) {
                    repository.getPlaybackPlan(drama.id, chapter.id, episodeIndex + 1).onSuccess { plan ->
                        activeVideoUrl = if (plan.delivery == "redirect" && !plan.directURL.isNullOrBlank()) {
                            plan.directURL
                        } else {
                            serverUrl.trimEnd('/') + plan.url
                        }
                    }.onFailure {
                        // Fallback stream proxy URL
                        activeVideoUrl = "${serverUrl.trimEnd('/')}/api/ui/playback/stream?id=${drama.id}&chapter=${chapter.id}"
                    }
                }
            }.onFailure {
                // Mock chapters if offline
                activeChapters = (1..80).map { ep ->
                    Chapter(id = "ch_$ep", title = "第 $ep 集")
                }
                activeVideoUrl = "${serverUrl.trimEnd('/')}/api/ui/playback/stream?id=${drama.id}"
            }
        }
    }

    LaunchedEffect(Unit) {
        repository.initServerUrl()
        loadDramas()
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> {
                repository.getFollowing().onSuccess { res ->
                    followingList = res.following
                    historyList = res.history
                }
            }
            2 -> {
                repository.getDownloadTasks().onSuccess { res ->
                    downloadTasks = res.tasks
                }
            }
        }
    }

    // If activeDrama is non-null, display fullscreen Player
    if (activeDrama != null) {
        PlayerScreen(
            drama = activeDrama!!,
            chapters = activeChapters,
            currentEpisodeIndex = activeEpisodeIndex,
            videoUrl = activeVideoUrl,
            viewerId = repository.apiClient.getViewerId(),
            danmakuList = danmakuList,
            onBack = { activeDrama = null },
            onEpisodeChange = { newIdx ->
                openDrama(activeDrama!!, newIdx)
            },
            onProgressUpdate = { pos, dur ->
                val chId = activeChapters.getOrNull(activeEpisodeIndex)?.id ?: "1"
                coroutineScope.launch {
                    repository.reportProgress(activeDrama!!.id, chId, pos, dur, pos >= dur - 2.0)
                }
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                JukuBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        dramas = dramas,
                        isLoading = isLoadingDramas,
                        serverUrl = serverUrl,
                        onDramaClick = { openDrama(it) },
                        onSearch = { query ->
                            coroutineScope.launch {
                                if (query.isBlank()) loadDramas()
                                else repository.searchDramas(query).onSuccess { dramas = it }
                            }
                        },
                        onCategorySelect = { category ->
                            loadDramas(category)
                        },
                        onRefresh = { loadDramas() }
                    )

                    1 -> FollowingScreen(
                        followingList = followingList,
                        historyList = historyList,
                        serverUrl = serverUrl,
                        onDramaClick = { dramaId ->
                            dramas.find { it.id == dramaId }?.let { openDrama(it) }
                                ?: openDrama(Drama(id = dramaId, title = "短剧"))
                        }
                    )

                    2 -> DownloadScreen(
                        tasks = downloadTasks,
                        onMergeClick = { id ->
                            coroutineScope.launch {
                                repository.triggerMerge(id)
                            }
                        }
                    )

                    3 -> SettingsScreen(
                        currentServerUrl = serverUrl,
                        currentAccount = currentAccount,
                        onSaveServerUrl = { newUrl ->
                            coroutineScope.launch {
                                repository.saveServerUrl(newUrl)
                            }
                        },
                        onTestConnection = {
                            coroutineScope.launch {
                                repository.checkConnection().onSuccess {
                                    connectionStatus = "连接正常 · 延迟 ${it.latencyMs}ms"
                                }.onFailure {
                                    connectionStatus = "连接异常: ${it.localizedMessage}"
                                }
                            }
                        },
                        onLogin = { username, password, callback ->
                            coroutineScope.launch {
                                repository.login(username, password).onSuccess {
                                    callback(null)
                                    connectionStatus = "已作为 ${it.username} 登录"
                                }.onFailure {
                                    callback(it.message ?: "登录失败，请检查账号密码")
                                }
                            }
                        },
                        onLogout = {
                            coroutineScope.launch {
                                repository.logout()
                                connectionStatus = "已退出登录"
                            }
                        },
                        connectionStatus = connectionStatus
                    )
                }
            }
        }
    }
}
