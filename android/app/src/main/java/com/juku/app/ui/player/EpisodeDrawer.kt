package com.juku.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juku.app.data.model.Chapter
import com.juku.app.ui.components.CategoryPill
import com.juku.app.ui.theme.*

private data class EpisodeSegment(val label: String, val range: IntRange)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDrawer(
    dramaTitle: String,
    chapters: List<Chapter>,
    currentEpisodeIndex: Int,
    onEpisodeSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val totalEpisodes = chapters.size
    val chunkSize = 30
    val segments = (0 until totalEpisodes step chunkSize).map { start ->
        val end = minOf(start + chunkSize, totalEpisodes)
        EpisodeSegment("${start + 1}-${end}集", start until end)
    }

    var selectedSegmentIndex by remember {
        mutableIntStateOf(currentEpisodeIndex / chunkSize)
    }

    val currentSegmentRange = segments.getOrNull(selectedSegmentIndex)?.range
        ?: (0 until totalEpisodes)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "选集",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(共${totalEpisodes}集)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                    Text(
                        text = "《$dramaTitle》",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CinemaGold,
                            fontSize = 12.sp
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segments tabs (1-30, 31-60, etc.)
            if (segments.size > 1) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(segments) { idx, segment ->
                        CategoryPill(
                            text = segment.label,
                            isSelected = selectedSegmentIndex == idx,
                            onClick = { selectedSegmentIndex = idx }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5-Column Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentSegmentRange.toList()) { epIdx ->
                    val isPlaying = epIdx == currentEpisodeIndex
                    val isWatched = epIdx < currentEpisodeIndex
                    val episodeNum = epIdx + 1

                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isPlaying -> CinemaRed.copy(alpha = 0.25f)
                                    isWatched -> DarkElevated
                                    else -> DarkCard
                                }
                            )
                            .border(
                                width = if (isPlaying) 1.5.dp else 1.dp,
                                color = if (isPlaying) CinemaRed else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onEpisodeSelect(epIdx)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "播放中",
                                    tint = CinemaRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$episodeNum",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = CinemaRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = "$episodeNum",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isWatched) TextMuted else TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "整季批量缓存",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("整季缓存", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onEpisodeSelect(0)
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
                ) {
                    Text("从第1集播放", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
