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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juku.app.data.model.DownloadTask
import com.juku.app.ui.theme.*
import java.util.Locale

@Composable
fun DownloadScreen(
    tasks: List<DownloadTask>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    operationMessage: String? = null,
    onMergeClick: (String) -> Unit
) {
    val mergeCandidates = tasks
        .filter { it.status == "success" && it.dramaId.isNotBlank() }
        .distinctBy { it.dramaId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid)
            .statusBarsPadding()
    ) {
        Text(
            text = "服务端下载任务",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading && tasks.isEmpty()) item {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CinemaRed)
                }
            }
            if (errorMessage != null) item {
                Text(errorMessage, color = CinemaRed, modifier = Modifier.padding(vertical = 12.dp))
            }
            if (operationMessage != null) item {
                Text(operationMessage, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (mergeCandidates.isNotEmpty()) item {
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
                                text = "合并已完成分集",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = CinemaGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "按短剧将已完成的连续分集合并为一个视频文件。",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        mergeCandidates.forEach { candidate ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "《${candidate.dramaTitle}》",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontSize = 12.sp),
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = { onMergeClick(candidate.dramaId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinemaGold),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "合并分集",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
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

            if (tasks.isEmpty() && !isLoading && errorMessage == null) {
                item {
                    Text(
                        text = "暂无下载任务",
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            items(tasks, key = { it.id }) { task ->
                DownloadTaskRow(task = task)
            }
        }
    }
}

@Composable
fun DownloadTaskRow(task: DownloadTask) {
    val status = when (task.status) {
        "queued" -> "等待下载"
        "parsing" -> "解析分集"
        "running" -> "正在下载"
        "success" -> "已完成"
        "failed" -> "下载失败"
        "canceled" -> "已取消"
        "paused" -> "已暂停"
        else -> task.status.ifBlank { "状态未知" }
    }
    val episodeTitle = task.title.ifBlank { task.episode.ifBlank { "下载任务" } }

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
                        text = "$episodeTitle · $status${if (task.progress > 0) " · ${task.progress}%" else ""}",
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaRed, fontSize = 11.sp)
                    )
                }

                if (task.status == "running" && task.speedBytesPerSecond > 0) {
                    Text(
                        text = formatDownloadSpeed(task.speedBytesPerSecond),
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaGold, fontSize = 11.sp)
                    )
                }
            }

            if (task.progress > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = CinemaRed,
                    trackColor = DarkElevated
                )
            }
            if (task.status == "failed" && task.error.isNotBlank()) {
                Text(
                    text = task.error,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun formatDownloadSpeed(bytesPerSecond: Double): String =
    if (bytesPerSecond >= 1024 * 1024) {
        String.format(Locale.getDefault(), "%.1f MB/s", bytesPerSecond / (1024 * 1024))
    } else {
        String.format(Locale.getDefault(), "%.0f KB/s", bytesPerSecond / 1024)
    }
