package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectEntity
import com.example.ui.components.BrandBadge
import com.example.ui.components.PhotoFrameTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimestampFormatter
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val recentProjects by viewModel.recentProjects.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadVideo(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        PhotoFrameTopBar(
            title = "PHOTO FRAME",
            subtitle = "MG Shankar Tech",
            canNavigateBack = false,
            actions = {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.HELP) },
                    modifier = Modifier.testTag("help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help & Tutorial",
                        tint = TextSecondary
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Error banner if any
            if (errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 1. HERO SECTION
            item {
                HeroSection(
                    onSelectVideo = { videoPickerLauncher.launch("video/*") },
                    onTryDemo = { viewModel.loadSampleDemoVideo() }
                )
            }

            // 2. QUICK ACTIONS
            item {
                SectionHeader(
                    title = "Studio Actions",
                    subtitle = "Tools designed for precision photography from video"
                )
                QuickActionsGrid(
                    onActionSelect = { actionType ->
                        when (actionType) {
                            QuickActionType.EXTRACT -> videoPickerLauncher.launch("video/*")
                            QuickActionType.BEST_PHOTOS -> {
                                if (viewModel.extractedFrames.value.isNotEmpty()) {
                                    viewModel.navigateTo(AppScreen.GALLERY)
                                } else {
                                    videoPickerLauncher.launch("video/*")
                                }
                            }
                            QuickActionType.PRECISION_INTERVAL -> {
                                videoPickerLauncher.launch("video/*")
                            }
                            QuickActionType.COMPARE -> {
                                if (viewModel.extractedFrames.value.size >= 2) {
                                    viewModel.launchComparisonStudio()
                                } else {
                                    viewModel.loadSampleDemoVideo()
                                }
                            }
                            QuickActionType.BATCH -> viewModel.navigateTo(AppScreen.BATCH_PROCESSING)
                            QuickActionType.PRESETS -> viewModel.navigateTo(AppScreen.SETTINGS)
                        }
                    }
                )
            }

            // 3. RECENT PROJECTS SECTION
            item {
                SectionHeader(
                    title = "Recent Extraction Projects",
                    subtitle = "${recentProjects.size} saved project sessions"
                )
            }

            if (recentProjects.isEmpty()) {
                item {
                    EmptyProjectsCard(
                        onSelectVideo = { videoPickerLauncher.launch("video/*") },
                        onTryDemo = { viewModel.loadSampleDemoVideo() }
                    )
                }
            } else {
                items(recentProjects, key = { it.id }) { project ->
                    RecentProjectCard(
                        project = project,
                        onReopen = { viewModel.reopenProject(project) },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
            }

            // 4. CREATOR BRAND FOOTER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BrandBadge()
                }
            }
        }
    }
}

@Composable
fun HeroSection(
    onSelectVideo: () -> Unit,
    onTryDemo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Animated Timeline-to-Photo Grid Graphic
            TimelineToPhotoGraphic()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Every Moment Deserves Its Perfect Frame",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Turn any video into a collection of stunning, millisecond-precise photographs with AI sharpness ranking.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSelectVideo,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("select_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Video",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onTryDemo,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("try_demo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Try Demo Video",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineToPhotoGraphic() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Timeline rail
            drawLine(
                color = Color(0xFF283556),
                start = Offset(20f, h / 2),
                end = Offset(w - 20f, h / 2),
                strokeWidth = 4f
            )

            // Millisecond tick marks
            val tickCount = 18
            val tickStep = (w - 40f) / tickCount
            for (i in 0..tickCount) {
                val x = 20f + i * tickStep
                val isMajor = i % 3 == 0
                val tickH = if (isMajor) 14f else 8f
                drawLine(
                    color = if (isMajor) Color(0xFF25D9F8) else Color(0xFF475569),
                    start = Offset(x, (h / 2) - (tickH / 2)),
                    end = Offset(x, (h / 2) + (tickH / 2)),
                    strokeWidth = if (isMajor) 3f else 1.5f
                )
            }

            // Highlight extracted photo frames along the timeline
            val frameXs = listOf(0.25f, 0.45f, 0.65f, 0.85f)
            for (frac in frameXs) {
                val cx = 20f + frac * (w - 40f)
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFF25D9F8), Color(0xFF7C5CFF))),
                    radius = 10f,
                    center = Offset(cx, h / 2)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(cx, h / 2)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "00:00:00.000",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = TextSecondary
            )
            Text(
                text = "PRECISION TIMELINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                ),
                color = NeonCyan
            )
            Text(
                text = "00:00:03.000",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = TextSecondary
            )
        }
    }
}

enum class QuickActionType {
    EXTRACT,
    BEST_PHOTOS,
    PRECISION_INTERVAL,
    COMPARE,
    BATCH,
    PRESETS
}

data class QuickActionItem(
    val type: QuickActionType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun QuickActionsGrid(
    onActionSelect: (QuickActionType) -> Unit
) {
    val items = listOf(
        QuickActionItem(
            QuickActionType.EXTRACT,
            "Extract Frames",
            "From any video",
            Icons.Default.CameraAlt,
            ElectricViolet
        ),
        QuickActionItem(
            QuickActionType.BEST_PHOTOS,
            "AI Situation Spotter",
            "Describe & find moment",
            Icons.Default.AutoAwesome,
            SoftGreen
        ),
        QuickActionItem(
            QuickActionType.PRECISION_INTERVAL,
            "Precision Interval",
            "Down to 1 millisecond",
            Icons.Default.ShutterSpeed,
            NeonCyan
        ),
        QuickActionItem(
            QuickActionType.COMPARE,
            "Compare Frames",
            "Side-by-side inspect",
            Icons.Default.Compare,
            Color(0xFFFBBF24)
        ),
        QuickActionItem(
            QuickActionType.BATCH,
            "Batch Processing",
            "Multi-video queue",
            Icons.Default.Queue,
            Color(0xFFEC4899)
        ),
        QuickActionItem(
            QuickActionType.PRESETS,
            "Interval Presets",
            "Sports, Cinema, Vlog",
            Icons.Default.Schedule,
            Color(0xFF818CF8)
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0 until items.size step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    item = items[i],
                    onClick = { onActionSelect(items[i].type) },
                    modifier = Modifier.weight(1f)
                )
                if (i + 1 < items.size) {
                    QuickActionCard(
                        item = items[i + 1],
                        onClick = { onActionSelect(items[i + 1].type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    item: QuickActionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick)
            .testTag("quick_action_${item.type.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.color.copy(alpha = 0.15f))
                    .border(1.dp, item.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RecentProjectCard(
    project: ProjectEntity,
    onReopen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onReopen)
            .testTag("recent_project_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${project.frameCount} frames extracted",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = ElectricViolet
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = TimestampFormatter.formatDurationMs(project.durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Project",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyProjectsCard(
    onSelectVideo: () -> Unit,
    onTryDemo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Previous Projects Yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Import a video from your gallery or test the engine with our demo video.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSelectVideo,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Choose Video")
                }
                OutlinedButton(
                    onClick = onTryDemo,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                ) {
                    Text("Run Demo")
                }
            }
        }
    }
}
