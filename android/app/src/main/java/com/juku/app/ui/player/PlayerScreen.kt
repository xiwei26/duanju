package com.juku.app.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.juku.app.data.model.Chapter
import com.juku.app.data.model.DanmakuItem
import com.juku.app.data.model.Drama
import com.juku.app.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    drama: Drama,
    chapters: List<Chapter>,
    currentEpisodeIndex: Int,
    videoUrl: String,
    startPositionSeconds: Double = 0.0,
    autoPlay: Boolean = true,
    requestHeaders: Map<String, String> = emptyMap(),
    danmakuList: List<DanmakuItem> = emptyList(),
    errorMessage: String? = null,
    isFollowed: Boolean = false,
    onBack: () -> Unit,
    onEpisodeChange: (Int) -> Unit,
    onFollowChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onProgressUpdate: (positionSeconds: Double, durationSeconds: Double) -> Unit,
    onPositionChange: (positionSeconds: Double, durationSeconds: Double) -> Unit,
    onPlaybackError: (responseCode: Int?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(requestHeaders) {
        // Create custom DataSource.Factory with required headers
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("JukuApp/1.0")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)

        if (requestHeaders.isNotEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(requestHeaders)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = autoPlay
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    var isPlaying by remember(videoUrl) { mutableStateOf(autoPlay) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDanmakuOn by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var lastReportedAtMs by remember(videoUrl) { mutableStateOf<Long?>(null) }

    fun publishPosition(report: Boolean) {
        if (videoUrl.isBlank()) return
        val position = exoPlayer.currentPosition.coerceAtLeast(0L) / 1000.0
        val duration = exoPlayer.duration.coerceAtLeast(0L) / 1000.0
        if (duration > 0) {
            onPositionChange(position, duration)
            if (report) {
                lastReportedAtMs = SystemClock.elapsedRealtime()
                onProgressUpdate(position, duration)
            }
        }
    }

    // Load media URL into ExoPlayer when it changes
    LaunchedEffect(videoUrl, exoPlayer, startPositionSeconds, autoPlay) {
        if (videoUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            if (startPositionSeconds.isFinite() && startPositionSeconds > 0) {
                exoPlayer.seekTo((startPositionSeconds * 1000).toLong())
            }
            exoPlayer.prepare()
            if (autoPlay) exoPlayer.play() else exoPlayer.pause()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            currentPosMs = 0L
            durationMs = 0L
        }
    }

    val currentOnError by rememberUpdatedState(onPlaybackError)
    val currentOnProgress by rememberUpdatedState(onProgressUpdate)
    DisposableEffect(exoPlayer, videoUrl) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val responseCode = generateSequence(error as Throwable?) { it.cause }
                    .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
                    .firstOrNull()?.responseCode
                currentOnError(responseCode)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && videoUrl.isNotBlank()) {
                    val duration = exoPlayer.duration.coerceAtLeast(0L) / 1000.0
                    if (duration > 0) currentOnProgress(duration, duration)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Periodic progress update
    LaunchedEffect(exoPlayer, videoUrl) {
        while (true) {
            if (videoUrl.isNotBlank()) {
                currentPosMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (durationMs > 0) {
                    onPositionChange(currentPosMs / 1000.0, durationMs / 1000.0)
                    val nowMs = SystemClock.elapsedRealtime()
                    if (exoPlayer.isPlaying && shouldReportPlaybackProgress(nowMs, lastReportedAtMs)) {
                        lastReportedAtMs = nowMs
                        onProgressUpdate(currentPosMs / 1000.0, durationMs / 1000.0)
                    }
                }
            }
            isPlaying = exoPlayer.isPlaying
            delay(1000)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer, videoUrl) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && videoUrl.isNotBlank()) {
                publishPosition(report = true)
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentChapter = chapters.getOrNull(currentEpisodeIndex)
    val episodeNum = currentEpisodeIndex + 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        if (videoUrl.isBlank() && errorMessage == null) {
            CircularProgressIndicator(
                color = CinemaRed,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (errorMessage != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(errorMessage, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("重试") }
            }
        }

        // Danmaku Overlay
        if (isDanmakuOn && danmakuList.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 120.dp, start = 16.dp, end = 16.dp)
            ) {
                danmakuList.take(3).forEach { danmaku ->
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = danmaku.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }

        // Top Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = drama.displayTitle(),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (currentChapter == null) "正在准备…" else "第 $episodeNum 集",
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaGold)
                    )
                }
            }

        }

        // Right Vertical Action Column
        if (chapters.isNotEmpty()) Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Episode Drawer Button
            FloatingActionButton(
                onClick = { showEpisodeDrawer = true },
                containerColor = DarkCard.copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "选集",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$episodeNum/${chapters.size}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Follow / Favorite Button
            FloatingActionButton(
                onClick = { onFollowChange(!isFollowed) },
                containerColor = DarkCard.copy(alpha = 0.85f),
                contentColor = if (isFollowed) CinemaRed else Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "追剧",
                        modifier = Modifier.size(20.dp)
                    )
                    Text("追剧", fontSize = 9.sp)
                }
            }

            // Danmaku Toggle
            if (danmakuList.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { isDanmakuOn = !isDanmakuOn },
                    containerColor = DarkCard.copy(alpha = 0.85f),
                    contentColor = if (isDanmakuOn) CinemaRed else Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "弹幕",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(if (isDanmakuOn) "开" else "关", fontSize = 9.sp)
                    }
                }
            }
        }

        // Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
        )

        // Bottom Controls Cluster
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Episode Title & Synopsis
            Text(
                text = currentChapter?.title ?: "正在准备播放",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
            Text(
                text = drama.desc.ifBlank { drama.intro.ifBlank { "精彩短剧" } },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Slider
            val progressFraction = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()) else 0f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentPosMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 11.sp)
                )
                Slider(
                    value = progressFraction,
                    enabled = videoUrl.isNotBlank() && durationMs > 0,
                    onValueChange = { frac ->
                        val targetMs = (frac * durationMs).toLong()
                        exoPlayer.seekTo(targetMs)
                        currentPosMs = targetMs
                        onPositionChange(targetMs / 1000.0, durationMs / 1000.0)
                    },
                    onValueChangeFinished = { publishPosition(report = true) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = CinemaRed,
                        activeTrackColor = CinemaRed,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 11.sp)
                )
            }

            // Bottom Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Toggle
                IconButton(
                    enabled = videoUrl.isNotBlank(),
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            publishPosition(report = true)
                        } else {
                            exoPlayer.play()
                        }
                        isPlaying = exoPlayer.isPlaying
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放暂停",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Speed Selector Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DarkCard)
                        .clickable(enabled = videoUrl.isNotBlank()) {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            exoPlayer.setPlaybackSpeed(playbackSpeed)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${playbackSpeed}X",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Next Episode Button
                if (currentEpisodeIndex + 1 < chapters.size) {
                    Button(
                        onClick = { onEpisodeChange(currentEpisodeIndex + 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("下一集 ▶", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Episode Drawer Sheet
        if (showEpisodeDrawer && chapters.isNotEmpty()) {
            EpisodeDrawer(
                dramaTitle = drama.displayTitle(),
                chapters = chapters,
                currentEpisodeIndex = currentEpisodeIndex,
                onEpisodeSelect = onEpisodeChange,
                onDismiss = { showEpisodeDrawer = false }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
