package com.juku.app.ui.download

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juku.app.data.model.DownloadTask
import com.juku.app.ui.theme.*

@Composable
fun DownloadScreen(
    tasks: List<DownloadTask>,
    onMergeClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 正在下载, 1: 已完成

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "下载管理",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
            )

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "全部暂停",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("全部暂停", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // Storage status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "剩余可用 128.5 GB · 已缓存 12.4 GB",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontSize = 12.sp)
                    )
                    Text(
                        text = "清理缓存",
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaRed, fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { 0.18f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = CinemaRed,
                    trackColor = DarkElevated
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Smart Merge Full Feature Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, CinemaGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "全集合并",
                                tint = CinemaGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "短剧全集智能无损合并",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = CinemaGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "探测分集编码与音频，一键极速无损拼装为单集超长全片，告别频繁切集卡顿与缓冲！",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "《重回1990当首富》(全85集已下载)",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontSize = 12.sp)
                            )

                            Button(
                                onClick = { onMergeClick("demo_drama_id") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CinemaGold
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "一键合并全集 ▶",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "📥 下载任务",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Task List (mocked with realistic data if empty)
            val displayTasks = if (tasks.isNotEmpty()) tasks else listOf(
                DownloadTask(
                    id = "task_1",
                    dramaId = "1",
                    dramaTitle = "普通弓箭手 第五季",
                    episode = "24",
                    title = "第24集：绝地反击",
                    status = "running",
                    progress = 68,
                    speedBytesPerSecond = 3.8 * 1024 * 1024,
                    downloadedBytes = 18 * 1024 * 1024,
                    totalBytes = 27 * 1024 * 1024
                ),
                DownloadTask(
                    id = "task_2",
                    dramaId = "2",
                    dramaTitle = "我在异界当领主",
                    episode = "5",
                    title = "第5集：神秘召唤",
                    status = "running",
                    progress = 24,
                    speedBytesPerSecond = 2.1 * 1024 * 1024,
                    downloadedBytes = 6 * 1024 * 1024,
                    totalBytes = 25 * 1024 * 1024
                )
            )

            items(displayTasks) { task ->
                DownloadTaskRow(task = task)
            }
        }
    }
}

@Composable
fun DownloadTaskRow(task: DownloadTask) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = task.dramaTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp)
                    )
                    Text(
                        text = "正在下载第 ${task.episode} 集 · ${task.progress}%",
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaRed, fontSize = 11.sp)
                    )
                }

                Text(
                    text = "⚡ 3.8 MB/s",
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaGold, fontSize = 11.sp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = CinemaRed,
                trackColor = DarkElevated
            )
        }
    }
}
