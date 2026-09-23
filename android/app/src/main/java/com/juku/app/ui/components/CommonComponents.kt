package com.juku.app.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.juku.app.data.model.Drama
import com.juku.app.ui.theme.*

@Composable
fun CategoryPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isSelected) Brush.horizontalGradient(listOf(CinemaRed, CinemaOrange))
                else Brush.horizontalGradient(listOf(DarkCard, DarkCard))
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else DarkBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
fun DramaCard(
    drama: Drama,
    serverUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
    isFollowed: Boolean = false,
    onFollowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f) // 3:4 aspect ratio
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

            // Top Episode Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = drama.displayEpisodeCount(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CinemaGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Bottom subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, DarkCard)
                        )
                    )
            )
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = drama.displayTitle(),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = drama.heat?.let { "🔥 $it" } ?: (drama.categoryName ?: "热播短剧"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextMuted
                )

                IconButton(
                    onClick = onFollowClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "追剧",
                        tint = CinemaRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun JukuBottomNavBar(
    selectedTab: Int,
    showDownloads: Boolean = true,
    onTabSelected: (Int) -> Unit
) {
    data class NavItem(val tab: Int, val label: String, val filled: ImageVector, val outlined: ImageVector)
    val items = buildList {
        add(NavItem(0, "剧库", Icons.Default.VideoLibrary, Icons.Outlined.VideoLibrary))
        add(NavItem(1, "追剧", Icons.Default.Bookmark, Icons.Outlined.BookmarkBorder))
        if (showDownloads) add(NavItem(2, "下载", Icons.Default.Download, Icons.Outlined.Download))
        add(NavItem(3, "我的", Icons.Default.Person, Icons.Outlined.PersonOutline))
    }

    Surface(
        color = DarkElevated.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedTab == item.tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onTabSelected(item.tab) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) item.filled else item.outlined,
                        contentDescription = item.label,
                        tint = if (isSelected) CinemaRed else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CinemaRed else TextSecondary
                        )
                    )
                }
            }
        }
    }
}
