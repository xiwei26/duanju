package com.juku.app.ui.following

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.juku.app.data.model.FollowingItem
import com.juku.app.ui.components.authenticatedImageRequest
import com.juku.app.ui.theme.*

@Composable
fun FollowingScreen(
    followingList: List<FollowingItem>,
    historyList: List<FollowingItem>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    serverUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    onDramaClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 追剧, 1: 历史

    val currentList = if (selectedTab == 0) followingList else historyList
    val continueWatching = historyList.firstOrNull { !it.completed }.takeIf { selectedTab == 1 }
    val visibleList = if (continueWatching == null) currentList else currentList.filterNot { it == continueWatching }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid)
            .statusBarsPadding()
    ) {
        // Top Header with Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented Tabs Pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, CircleShape)
                    .padding(4.dp)
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selectedTab == 0) CinemaRed else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "我的追剧 (${followingList.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (selectedTab == 0) TextPrimary else TextSecondary,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selectedTab == 1) CinemaRed else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "观看历史 (${historyList.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (selectedTab == 1) TextPrimary else TextSecondary,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading && currentList.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CinemaRed)
                    }
                }
            }
            if (errorMessage != null) {
                item {
                    Text(errorMessage, color = CinemaRed, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            // Continue Watching Hero Card
            if (continueWatching != null) {
                item {
                    ContinueWatchingCard(
                    item = continueWatching,
                    serverUrl = serverUrl,
                    requestHeaders = requestHeaders,
                        onClick = { onDramaClick(continueWatching.dramaId) }
                    )
                }
            }

            item {
                Text(
                    text = if (selectedTab == 0) "📌 追剧清单" else "🕒 最近历史",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            if (currentList.isEmpty() && !isLoading && errorMessage == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "暂无追剧记录，快去剧库挑选吧~" else "暂无历史记录",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(visibleList) { item ->
                    FollowingItemRow(
                        item = item,
                        serverUrl = serverUrl,
                        requestHeaders = requestHeaders,
                        onClick = { onDramaClick(item.dramaId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: FollowingItem,
    serverUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    onClick: () -> Unit
) {
    val progress = if (item.duration > 0) {
        (item.position / item.duration).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = authenticatedImageRequest(
                    LocalContext.current,
                    item.coverUrl,
                    requestHeaders
                ),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 65.dp, height = 85.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkElevated)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CinemaGold.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "上次看到",
                        color = CinemaGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.episode?.let { "第 $it 集 · 观看至 ${(progress * 100).toInt()}%" }
                        ?: "观看至 ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = CinemaRed,
                    trackColor = DarkElevated
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "继续看",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text("继续看", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FollowingItemRow(
    item: FollowingItem,
    serverUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = authenticatedImageRequest(
                LocalContext.current,
                item.coverUrl,
                requestHeaders
            ),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 50.dp, height = 66.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkElevated)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.episode?.let { "看到第 $it 集" } ?: "已收藏",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 11.sp)
            )
            Text(
                text = (item.totalEpisode.takeIf { it > 0 } ?: item.knownEpisodes.takeIf { it > 0 })
                    ?.let { "共${it}集" }
                    ?: "集数未知",
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaGold, fontSize = 10.sp)
            )
        }

        OutlinedButton(
            onClick = onClick,
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaRed),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Text("继续看 ▶", color = CinemaRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
