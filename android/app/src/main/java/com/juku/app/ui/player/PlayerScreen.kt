package com.juku.app.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.juku.app.data.model.Chapter
import com.juku.app.data.model.DanmakuItem
import com.juku.app.data.model.Drama
import com.juku.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    drama: Drama,
    chapters: List<Chapter>,
    currentEpisodeIndex: Int,
    videoUrl: String,
    danmakuList: List<DanmakuItem> = emptyList(),
    onBack: () -> Unit,
    onEpisodeChange: (Int) -> Unit,
    onProgressUpdate: (positionSeconds: Double, durationSeconds: Double) -> Unit
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDanmakuOn by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFollowed by remember { mutableStateOf(false) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }

    // Load media URL into ExoPlayer when it changes
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Periodic progress update
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (durationMs > 0) {
                    onProgressUpdate(currentPosMs / 1000.0, durationMs / 1000.0)
                }
            }
            isPlaying = exoPlayer.isPlaying
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
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
            modifier = Modifier.fillMaxSize()
        )

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
                        imageVector = Icons.Default.ArrowBack,
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
                        text = "第 $episodeNum 集",
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaGold)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quality Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DarkCard.copy(alpha = 0.8f))
                        .border(1.dp, DarkBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "1080P 超清",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { /* Rotate */ }) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "横屏",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Right Vertical Action Column
        Column(
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
                onClick = { isFollowed = !isFollowed },
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

            // Download Button
            FloatingActionButton(
                onClick = { /* Start Download */ },
                containerColor = DarkCard.copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "缓存",
                        modifier = Modifier.size(20.dp)
                    )
                    Text("缓存", fontSize = 9.sp)
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
                text = currentChapter?.title ?: "第 $episodeNum 集",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
            Text(
                text = drama.desc ?: drama.intro ?: "高能短剧，精彩绝伦，逆袭归来！",
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
                    onValueChange = { frac ->
                        val targetMs = (frac * durationMs).toLong()
                        exoPlayer.seekTo(targetMs)
                    },
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
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
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
                        .clickable {
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
        if (showEpisodeDrawer) {
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
    return String.format("%02d:%02d", minutes, seconds)
}
