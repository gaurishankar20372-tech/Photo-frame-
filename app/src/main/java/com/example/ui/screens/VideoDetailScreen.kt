package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PhotoFrameTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatChip
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimestampFormatter
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@Composable
fun VideoDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val metadata by viewModel.videoMetadata.collectAsState()
    val config by viewModel.extractionConfig.collectAsState()
    val estimatedFrames by viewModel.totalEstimatedFrames.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PhotoFrameTopBar(
            title = "Video Analysis",
            subtitle = metadata?.filename ?: "Selected Video",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() }
        )

        if (metadata == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No video metadata found", color = TextSecondary)
            }
            return
        }

        val meta = metadata!!

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. VIDEO PREVIEW CONTAINER
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Video Thumbnail",
                                tint = NeonCyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(54.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = TimestampFormatter.formatDurationMs(meta.durationMs),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = meta.filename,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 2
                        )
                    }
                }
            }

            // 2. VIDEO METADATA GRID
            item {
                SectionHeader(
                    title = "Source Telemetry & Codec",
                    subtitle = "Decoded by Android native media pipeline"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            label = "RESOLUTION",
                            value = "${meta.width} × ${meta.height}",
                            icon = Icons.Default.HighQuality,
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "FRAME RATE",
                            value = "${String.format("%.1f", meta.fps)} FPS",
                            icon = Icons.Default.ShutterSpeed,
                            accentColor = ElectricViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            label = "DURATION",
                            value = TimestampFormatter.formatDurationMs(meta.durationMs),
                            icon = Icons.Default.Schedule,
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "FILE SIZE",
                            value = TimestampFormatter.formatFileSize(meta.sizeBytes),
                            icon = Icons.Default.DataUsage,
                            accentColor = ElectricViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            label = "ASPECT RATIO",
                            value = meta.aspectRatioString,
                            icon = Icons.Default.AspectRatio,
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "CONTAINER",
                            value = meta.mimeType.substringAfterLast('/'),
                            icon = Icons.Default.Movie,
                            accentColor = ElectricViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. EXTRACTION PLAN OVERVIEW
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Extraction Plan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Surface(
                                color = NeonCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "~$estimatedFrames Frames",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Interval: ${config.intervalUs / 1000.0} ms  •  Range: ${TimestampFormatter.formatUsToHhMmSsMmm(config.startTimestampUs)} → ${TimestampFormatter.formatUsToHhMmSsMmm(config.endTimestampUs)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextSecondary
                        )
                    }
                }
            }

            // 4. ACTION BUTTONS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.startExtraction() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_quick_extraction_button")
                    ) {
                        Text(
                            text = "Extract All Frames Now",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.PRECISION_EDITOR) },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("fine_tune_timeline_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fine-Tune Timeline & Milliseconds",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
