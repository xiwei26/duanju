package com.juku.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.juku.app.data.model.*
import com.juku.app.data.repository.JukuRepository
import com.juku.app.ui.components.JukuBottomNavBar
import com.juku.app.ui.download.DownloadScreen
import com.juku.app.ui.following.FollowingScreen
import com.juku.app.ui.home.HomeScreen
import com.juku.app.ui.player.PlayerScreen
import com.juku.app.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException

@Composable
fun MainScreen(repository: JukuRepository) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val serverUrl by repository.serverUrlFlow.collectAsState(initial = "")
    val currentAccount by repository.currentAccount.collectAsState(initial = null)

    var dramas by remember { mutableStateOf<List<Drama>>(emptyList()) }
    var categoryOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingDramas by remember { mutableStateOf(false) }
    var followingList by remember { mutableStateOf<List<FollowingItem>>(emptyList()) }
    var historyList by remember { mutableStateOf<List<FollowingItem>>(emptyList()) }
    var downloadTasks by remember { mutableStateOf<List<DownloadTask>>(emptyList()) }
    var isLoadingFollowing by remember { mutableStateOf(false) }
    var followingError by remember { mutableStateOf<String?>(null) }
    var isLoadingDownloads by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var mergeStatus by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf<String?>("正在连接服务端…") }
    var isServerConnected by remember { mutableStateOf(false) }

    var activeDrama by remember { mutableStateOf<Drama?>(null) }
    var activeChapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var activeEpisodeIndex by remember { mutableIntStateOf(0) }
    var activeSession by remember { mutableStateOf("") }
    var activeRun by remember { mutableLongStateOf(0L) }
    var playbackVersion by remember { mutableLongStateOf(0L) }
    var progressSequence by remember { mutableLongStateOf(0L) }
    var activeVideoUrl by remember { mutableStateOf("") }
    var playbackHeaders by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var serverRequestHeaders by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var playbackGeneration by remember { mutableLongStateOf(0L) }
    var episodeGeneration by remember { mutableLongStateOf(0L) }
    var libraryGeneration by remember { mutableLongStateOf(0L) }
    var followingGeneration by remember { mutableLongStateOf(0L) }
    var playbackRecoveryAttempts by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableDoubleStateOf(0.0) }
    var currentDuration by remember { mutableDoubleStateOf(0.0) }
    var requestedStartPosition by remember { mutableDoubleStateOf(0.0) }
    var requestedAutoPlay by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var sessionRecoveryAttempts by remember { mutableIntStateOf(0) }
    val progressMutex = remember { Mutex() }

    fun clearAccountData() {
        dramas = emptyList()
        categoryOptions = emptyList()
        followingList = emptyList()
        historyList = emptyList()
        downloadTasks = emptyList()
        isLoadingDramas = false
        isLoadingFollowing = false
        isLoadingDownloads = false
        followingError = null
        downloadError = null
        mergeStatus = null
        libraryGeneration += 1
        followingGeneration += 1
    }

    fun reportCurrentProgress() {
        val session = activeSession
        val run = activeRun
        val episode = activeEpisodeIndex + 1
        val position = currentPosition
        val duration = currentDuration
        if (session.isBlank() || run <= 0 || duration <= 0) return
        progressSequence += 1
        val sequence = progressSequence
        coroutineScope.launch {
            progressMutex.withLock {
                repository.reportProgress(
                    session, run, sequence, episode, position, duration,
                    completed = position >= duration - 2.0
                )
            }
        }
    }

    fun loadDramas(category: String? = null) {
        libraryGeneration += 1
        val generation = libraryGeneration
        coroutineScope.launch {
            isLoadingDramas = true
            while (generation == libraryGeneration) {
                val result = repository.getDramas(category)
                if (generation != libraryGeneration) break
                result.onSuccess { response ->
                    dramas = response.data
                    if (category.isNullOrBlank()) {
                        categoryOptions = response.data.map(Drama::displayCategory)
                            .filter(String::isNotBlank)
                            .distinct()
                            .take(8)
                    }
                    connectionStatus = if (response.loading) "剧库正在加载…" else "已连接 · 共 ${response.data.size} 部短剧"
                    if (!response.loading) isLoadingDramas = false
                }.onFailure {
                    connectionStatus = "剧库加载失败：${it.message ?: "未知错误"}"
                    isLoadingDramas = false
                }
                if (result.isFailure || result.getOrNull()?.loading != true) break
                delay(1500)
            }
        }
    }

    fun loadFollowing() {
        followingGeneration += 1
        val generation = followingGeneration
        isLoadingFollowing = true
        followingError = null
        coroutineScope.launch {
            repository.getFollowing()
                .onSuccess {
                    if (generation != followingGeneration) return@onSuccess
                    fun withCover(item: FollowingItem): FollowingItem {
                        val cover = dramas.find { drama -> drama.id == item.dramaId }
                            ?.displayCover(serverUrl)
                            .orEmpty()
                        return item.copy(coverUrl = cover)
                    }
                    followingList = it.following.map(::withCover)
                    historyList = it.history.map(::withCover)
                    isLoadingFollowing = false
                }
                .onFailure {
                    if (generation != followingGeneration) return@onFailure
                    followingList = emptyList()
                    historyList = emptyList()
                    isLoadingFollowing = false
                    followingError = it.message ?: "追剧记录加载失败"
                }
        }
    }

    fun setFollowing(drama: Drama, saved: Boolean) {
        coroutineScope.launch {
            repository.setFollowing(drama.id, saved)
                .onSuccess {
                    connectionStatus = if (saved) "已加入追剧" else "已取消追剧"
                    loadFollowing()
                }
                .onFailure { connectionStatus = "追剧操作失败：${it.message ?: "未知错误"}" }
        }
    }

    fun loadEpisode(
        session: String,
        episodeIndex: Int,
        start: Double = 0.0,
        mode: String = "proxy",
        autoPlay: Boolean = true
    ) {
        if (session.isBlank() || activeDrama == null) return
        episodeGeneration += 1
        val generation = episodeGeneration
        playbackVersion += 2 // Native fallback consumes the intermediate version.
        val version = playbackVersion
        activeVideoUrl = ""
        activeEpisodeIndex = episodeIndex
        activeRun = 0
        currentPosition = start
        requestedStartPosition = start
        requestedAutoPlay = autoPlay
        currentDuration = 0.0
        playbackError = null
        coroutineScope.launch {
            repository.getPlaybackPlan(session, episodeIndex + 1, start, version, mode)
                .onSuccess { plan ->
                    if (activeSession != session || generation != episodeGeneration || activeDrama == null) return@onSuccess
                    activeRun = plan.run
                    playbackHeaders = repository.playbackHeaders()
                    activeVideoUrl = plan.resolvedUrl(serverUrl)
                    if (activeVideoUrl.isBlank()) playbackError = "服务端未返回可播放地址"
                }.onFailure {
                    if (activeSession != session || generation != episodeGeneration || activeDrama == null) return@onFailure
                    playbackError = "播放准备失败：${it.message ?: "未知错误"}"
                }
        }
    }

    fun openDrama(
        drama: Drama,
        requestedEpisodeIndex: Int? = null,
        requestedPosition: Double? = null,
        recoveringSession: Boolean = false
    ) {
        playbackGeneration += 1
        episodeGeneration += 1
        val generation = playbackGeneration
        val previousSession = activeSession
        val previousProgress = if (previousSession.isNotBlank() && activeRun > 0 && currentDuration > 0) {
            progressSequence += 1
            PlaybackProgress(
                activeRun, progressSequence, activeEpisodeIndex + 1, currentPosition, currentDuration,
                completed = currentPosition >= currentDuration - 2.0
            )
        } else null
        activeSession = ""
        activeDrama = drama
        activeChapters = emptyList()
        activeVideoUrl = ""
        playbackError = null
        playbackRecoveryAttempts = 0
        if (!recoveringSession) sessionRecoveryAttempts = 0
        coroutineScope.launch {
            if (previousSession.isNotBlank()) {
                progressMutex.withLock { repository.closePlayback(previousSession, previousProgress) }
            }
            repository.openPlayback(drama.id).onSuccess { opened ->
                if (generation != playbackGeneration) {
                    repository.closePlayback(opened.session)
                    return@onSuccess
                }
                if (opened.episodes.isEmpty()) {
                    repository.closePlayback(opened.session)
                    activeDrama = null
                    connectionStatus = "这部短剧暂无可播放分集"
                    return@onSuccess
                }
                activeSession = opened.session
                activeChapters = opened.episodes.map(PlaybackEpisode::toChapter)
                progressSequence = 0
                playbackVersion = 0
                val episodeIndex = if (requestedEpisodeIndex != null) {
                    requestedEpisodeIndex.coerceIn(activeChapters.indices)
                } else {
                    (opened.initialIndex - 1).coerceIn(activeChapters.indices)
                }
                loadEpisode(
                    opened.session,
                    episodeIndex,
                    requestedPosition ?: opened.initialPosition,
                    autoPlay = !opened.resumePaused
                )
            }.onFailure {
                if (generation != playbackGeneration) return@onFailure
                activeDrama = null
                activeSession = ""
                connectionStatus = "无法打开短剧：${it.message ?: "未知错误"}"
            }
        }
    }

    fun closePlayer() {
        playbackGeneration += 1
        episodeGeneration += 1
        val session = activeSession
        val finalProgress = if (session.isNotBlank() && activeRun > 0 && currentDuration > 0) {
            progressSequence += 1
            PlaybackProgress(
                activeRun, progressSequence, activeEpisodeIndex + 1, currentPosition, currentDuration,
                completed = currentPosition >= currentDuration - 2.0
            )
        } else null
        activeDrama = null
        activeSession = ""
        activeVideoUrl = ""
        activeRun = 0
        if (session.isNotBlank()) coroutineScope.launch {
            progressMutex.withLock { repository.closePlayback(session, finalProgress) }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repository.initServerUrl() }
                .onSuccess {
                    isServerConnected = true
                    serverRequestHeaders = repository.playbackHeaders()
                loadDramas()
                loadFollowing()
            }
            .onFailure {
                isServerConnected = false
                connectionStatus = "连接失败：${it.message ?: "请检查服务地址"}"
            }
    }

    LaunchedEffect(currentAccount?.requirePasswordChange) {
        if (currentAccount?.requirePasswordChange == true) {
            clearAccountData()
            closePlayer()
            selectedTab = 3
            connectionStatus = "请先修改初始密码"
        }
    }

    LaunchedEffect(currentAccount?.onlineOnly) {
        if (currentAccount?.onlineOnly == true && selectedTab == 2) selectedTab = 0
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isForeground = true
                Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(activeSession, isForeground) {
        val session = activeSession
        if (session.isBlank() || !isForeground) return@LaunchedEffect
        while (activeSession == session) {
            repository.heartbeatPlayback(session).onFailure { error ->
                if ((error as? HttpException)?.code() == 410 && activeSession == session) {
                    val drama = activeDrama ?: return@onFailure
                    if (sessionRecoveryAttempts < 1) {
                        sessionRecoveryAttempts += 1
                        openDrama(drama, activeEpisodeIndex, currentPosition, recoveringSession = true)
                    } else {
                        playbackError = "播放会话已过期，请重新打开本剧"
                    }
                }
            }
            delay(20_000)
        }
    }

    LaunchedEffect(activeSession) {
        val session = activeSession
        if (session.isBlank()) return@LaunchedEffect
        try {
            awaitCancellation()
        } finally {
            // Normal navigation clears activeSession and closes explicitly. If the
            // composition itself is destroyed, close the still-active server slot.
            if (activeSession == session) {
                val finalProgress = if (activeRun > 0 && currentDuration > 0) {
                    progressSequence += 1
                    PlaybackProgress(
                        activeRun, progressSequence, activeEpisodeIndex + 1,
                        currentPosition, currentDuration,
                        completed = currentPosition >= currentDuration - 2.0
                    )
                } else null
                withContext(NonCancellable + Dispatchers.IO) {
                    withTimeoutOrNull(5_000) {
                        progressMutex.withLock { repository.closePlayback(session, finalProgress) }
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> loadFollowing()

            2 -> while (selectedTab == 2) {
                isLoadingDownloads = downloadTasks.isEmpty()
                downloadError = null
                val result = repository.getDownloadTasks()
                result.onSuccess {
                    downloadTasks = it.data
                    isLoadingDownloads = false
                }
                    .onFailure {
                        downloadTasks = emptyList()
                        isLoadingDownloads = false
                        downloadError = it.message ?: "下载任务加载失败"
                    }
                if (result.isFailure) break
                delay(2000)
            }
        }
    }

    if (activeDrama != null) {
        BackHandler(onBack = ::closePlayer)
        PlayerScreen(
            drama = activeDrama!!,
            chapters = activeChapters,
            currentEpisodeIndex = activeEpisodeIndex,
            videoUrl = activeVideoUrl,
            startPositionSeconds = requestedStartPosition,
            autoPlay = requestedAutoPlay,
            requestHeaders = playbackHeaders,
            errorMessage = playbackError,
            isFollowed = followingList.any { it.dramaId == activeDrama!!.id && it.saved },
            onBack = ::closePlayer,
            onEpisodeChange = { newIndex ->
                reportCurrentProgress()
                playbackRecoveryAttempts = 0
                sessionRecoveryAttempts = 0
                loadEpisode(activeSession, newIndex)
            },
            onFollowChange = { saved -> activeDrama?.let { setFollowing(it, saved) } },
            onRetry = {
                val drama = activeDrama
                if (drama != null) openDrama(drama, activeEpisodeIndex, currentPosition)
            },
            onPlaybackError = { responseCode ->
                val drama = activeDrama
                if (drama != null && responseCode == 410 && sessionRecoveryAttempts < 1) {
                    sessionRecoveryAttempts += 1
                    openDrama(drama, activeEpisodeIndex, currentPosition, recoveringSession = true)
                } else if (responseCode != 410 && playbackRecoveryAttempts < 2) {
                    playbackRecoveryAttempts += 1
                    loadEpisode(
                        activeSession, activeEpisodeIndex, currentPosition,
                        mode = if (playbackRecoveryAttempts == 1) "compatible" else "legacy"
                    )
                } else {
                    playbackError = if (responseCode == 410) "播放会话已过期，请重试" else "视频播放失败，请重试或选择其他分集"
                }
            },
            onPositionChange = { position, duration ->
                currentPosition = position
                currentDuration = duration
            },
            onProgressUpdate = { position, duration ->
                if (activeSession.isNotBlank() && activeRun > 0 && duration > 0) {
                    currentPosition = position
                    currentDuration = duration
                    reportCurrentProgress()
                }
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            JukuBottomNavBar(
                selectedTab = selectedTab,
                showDownloads = currentAccount?.onlineOnly != true,
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
                    connectionStatus = connectionStatus,
                    isConnected = isServerConnected,
                    categoryOptions = categoryOptions,
                    requestHeaders = serverRequestHeaders,
                    followedDramaIds = followingList.filter { it.saved }.mapTo(mutableSetOf()) { it.dramaId },
                    onDramaClick = { openDrama(it) },
                    onFollowClick = ::setFollowing,
                    onSearch = { query ->
                        if (query.isBlank()) {
                            loadDramas()
                        } else {
                            libraryGeneration += 1
                            isLoadingDramas = false
                            val generation = libraryGeneration
                            coroutineScope.launch {
                                repository.searchDramas(query)
                                    .onSuccess { if (generation == libraryGeneration) dramas = it }
                                    .onFailure { if (generation == libraryGeneration) connectionStatus = "搜索失败：${it.message}" }
                            }
                        }
                    },
                    onCategorySelect = { loadDramas(it) },
                    onRefresh = { loadDramas() }
                )

                1 -> FollowingScreen(
                    followingList = followingList,
                    historyList = historyList,
                    isLoading = isLoadingFollowing,
                    errorMessage = followingError,
                    serverUrl = serverUrl,
                    requestHeaders = serverRequestHeaders,
                    onDramaClick = { dramaId ->
                        dramas.find { it.id == dramaId }?.let(::openDrama)
                            ?: openDrama(Drama(id = dramaId, title = "短剧"))
                    }
                )

                2 -> DownloadScreen(
                    tasks = downloadTasks,
                    isLoading = isLoadingDownloads,
                    errorMessage = downloadError,
                    operationMessage = mergeStatus,
                    onMergeClick = { dramaId ->
                        mergeStatus = "正在提交合并任务…"
                        coroutineScope.launch {
                            repository.triggerMerge(dramaId)
                                .onSuccess { mergeStatus = "合并任务已提交，请留意任务状态" }
                                .onFailure { mergeStatus = "合并提交失败：${it.message}" }
                        }
                    }
                )

                3 -> SettingsScreen(
                    currentServerUrl = serverUrl,
                    currentAccount = currentAccount,
                    onSaveServerUrl = { newUrl ->
                        coroutineScope.launch {
                            runCatching { repository.saveServerUrl(newUrl) }
                                .onSuccess {
                                    isServerConnected = true
                                    clearAccountData()
                                    serverRequestHeaders = repository.playbackHeaders()
                                    connectionStatus = "服务地址已保存"
                                    loadDramas()
                                    loadFollowing()
                                }
                                .onFailure {
                                    isServerConnected = false
                                    connectionStatus = "地址无效：${it.message}"
                                }
                        }
                    },
                    onTestConnection = {
                        coroutineScope.launch {
                            repository.checkConnection()
                                .onSuccess {
                                    isServerConnected = true
                                    connectionStatus = "连接正常 · 延迟 ${it.latencyMs}ms"
                                }
                                .onFailure {
                                    isServerConnected = false
                                    connectionStatus = "连接异常：${it.message}"
                                }
                        }
                    },
                    onLogin = { username, password, callback ->
                        coroutineScope.launch {
                            repository.login(username, password)
                                .onSuccess {
                                    clearAccountData()
                                    serverRequestHeaders = repository.playbackHeaders()
                                    callback(null)
                                    connectionStatus = "已作为 ${it.username} 登录"
                                    if (!it.requirePasswordChange) {
                                        loadDramas()
                                        loadFollowing()
                                    }
                                }
                                .onFailure { callback(it.message ?: "登录失败，请检查账号密码") }
                        }
                    },
                    onLogout = {
                        coroutineScope.launch {
                            repository.logout()
                                .onSuccess {
                                    clearAccountData()
                                    serverRequestHeaders = repository.playbackHeaders()
                                    connectionStatus = "已退出登录"
                                    loadDramas()
                                    loadFollowing()
                                }
                                .onFailure { connectionStatus = "退出失败：${it.message}" }
                        }
                    },
                    onChangePassword = { password, newPassword, callback ->
                        coroutineScope.launch {
                            repository.changePassword(password, newPassword)
                                .onSuccess {
                                    clearAccountData()
                                    serverRequestHeaders = repository.playbackHeaders()
                                    callback(null)
                                    connectionStatus = "密码已更新"
                                    loadDramas()
                                    loadFollowing()
                                }
                                .onFailure { callback(it.message ?: "密码修改失败") }
                        }
                    },
                    connectionOk = isServerConnected,
                    connectionStatus = connectionStatus
                )
            }
        }
    }
}
