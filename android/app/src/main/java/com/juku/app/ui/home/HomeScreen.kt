package com.juku.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.juku.app.data.model.Drama
import com.juku.app.ui.components.CategoryPill
import com.juku.app.ui.components.DramaCard
import com.juku.app.ui.components.authenticatedImageRequest
import com.juku.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    dramas: List<Drama>,
    isLoading: Boolean,
    serverUrl: String,
    connectionStatus: String? = null,
    isConnected: Boolean = false,
    categoryOptions: List<String> = emptyList(),
    requestHeaders: Map<String, String> = emptyMap(),
    followedDramaIds: Set<String> = emptySet(),
    onDramaClick: (Drama) -> Unit,
    onFollowClick: (Drama, Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = listOf("全部") + categoryOptions

    val featuredDrama = dramas.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid)
            .statusBarsPadding()
    ) {
        // Top App Bar & Search Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "果果剧库",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = CinemaRed,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Server Online Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background((if (isConnected) SuccessGreen else CinemaRed).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) SuccessGreen else CinemaRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isConnected) "服务在线" else "未连接",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isConnected) SuccessGreen else CinemaRed,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = TextSecondary
                )
            }
        }

        if (connectionStatus != null && !isConnected) {
            Text(
                text = connectionStatus,
                color = if (connectionStatus.contains("加载")) TextSecondary else CinemaRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Search Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(CircleShape)
                .background(DarkCard)
                .border(1.dp, DarkBorder, CircleShape)
                .padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isBlank()) onSearch("")
                    else if (it.length >= 2) {
                        selectedCategory = null
                        onSearch(it)
                    }
                },
                placeholder = {
                    Text(
                        "搜索红果短剧、热播剧名...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onSearch("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Horizontal Category Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (selectedCategory == null && cat == "全部") || selectedCategory == cat
                CategoryPill(
                    text = cat,
                    isSelected = isSelected,
                    onClick = {
                        searchQuery = ""
                        selectedCategory = if (cat == "全部") null else cat
                        onCategorySelect(selectedCategory)
                    }
                )
            }
        }

        // Main Content: Grid with Featured Banner at top
        if (isLoading && dramas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinemaRed)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Featured Hero Banner
                if (featuredDrama != null && searchQuery.isBlank() && selectedCategory == null) {
                    item(span = { GridItemSpan(2) }) {
                        HeroBanner(
                            drama = featuredDrama,
                            serverUrl = serverUrl,
                            requestHeaders = requestHeaders,
                            onClick = { onDramaClick(featuredDrama) }
                        )
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 热门推荐",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "共 ${dramas.size} 部短剧",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }

                val visibleDramas = if (featuredDrama != null && searchQuery.isBlank() && selectedCategory == null) {
                    dramas.drop(1)
                } else {
                    dramas
                }
                items(visibleDramas) { drama ->
                    DramaCard(
                        drama = drama,
                        serverUrl = serverUrl,
                        requestHeaders = requestHeaders,
                        onClick = { onDramaClick(drama) },
                        isFollowed = drama.id in followedDramaIds,
                        onFollowClick = { onFollowClick(drama, drama.id !in followedDramaIds) }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroBanner(
    drama: Drama,
    serverUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = authenticatedImageRequest(
                LocalContext.current,
                drama.displayCover(serverUrl),
                requestHeaders
            ),
            contentDescription = drama.displayTitle(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CinemaGold)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "推荐",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = drama.heat?.let { "🔥 $it" } ?: drama.displayCategory(),
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaGold)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = drama.displayTitle(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "立即播放",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = drama.displayEpisodeCount(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                )
            }
        }
    }
}
