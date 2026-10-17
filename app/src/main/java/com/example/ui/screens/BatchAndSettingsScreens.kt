package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.BackendStatus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PresetRepository
import com.example.model.BatchJob
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
fun BatchProcessingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val batchQueue by viewModel.batchQueue.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addToBatchQueue(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PhotoFrameTopBar(
            title = "Batch Processing Queue",
            subtitle = "${batchQueue.size} Videos in Queue",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sequential Video Extraction",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Add multiple videos to extract frame collections without freezing your device memory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { pickerLauncher.launch("video/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Video to Queue")
                        }
                    }
                }
            }

            if (batchQueue.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Queue is currently empty.\nTap 'Add Video to Queue' above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(batchQueue, key = { it.id }) { job ->
                    BatchJobItemCard(
                        job = job,
                        onProcess = {
                            viewModel.loadVideo(Uri.parse(job.videoUriString))
                        },
                        onRemove = { viewModel.removeBatchJob(job.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BatchJobItemCard(
    job: BatchJob,
    onProcess: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.videoTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "Duration: ${TimestampFormatter.formatDurationMs(job.durationMs)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                )
            }

            Button(
                onClick = onProcess,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Process", style = MaterialTheme.typography.labelSmall)
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val backendUrl by viewModel.renderBackendUrl.collectAsState()
    val backendStatus by viewModel.backendStatus.collectAsState()
    val useCloudEnhance by viewModel.useRenderCloudEnhance.collectAsState()
    var localUrlText by remember(backendUrl) { mutableStateOf(backendUrl) }
    var showDeployDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PhotoFrameTopBar(
            title = "Settings & Cloud Engine",
            subtitle = "Render Backend & Studio Preferences",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Render Cloud Backend Section
            item {
                SectionHeader(
                    title = "Render Cloud Backend (रेंडर बैकएंड)",
                    subtitle = "Deploy on Render (render.com) for cloud-accelerated DSLR processing"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth().testTag("render_backend_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Status Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val (icon, tint, statusText, subText) = when (val s = backendStatus) {
                                is BackendStatus.Connected -> StatusDisplay(
                                    Icons.Default.CloudDone,
                                    SoftGreen,
                                    "Connected (${s.latencyMs}ms)",
                                    s.serviceName
                                )
                                is BackendStatus.Connecting -> StatusDisplay(
                                    Icons.Default.Sync,
                                    Color(0xFFFFB300),
                                    "Connecting to Render...",
                                    "Pinging /api/health"
                                )
                                is BackendStatus.Error -> StatusDisplay(
                                    Icons.Default.CloudOff,
                                    Color(0xFFFF5252),
                                    "Connection Failed",
                                    s.message
                                )
                                is BackendStatus.Disconnected -> StatusDisplay(
                                    Icons.Default.Cloud,
                                    TextSecondary,
                                    "Not Connected",
                                    "Enter Render URL and test connection"
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = tint
                                )
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // URL Input
                        OutlinedTextField(
                            value = localUrlText,
                            onValueChange = {
                                localUrlText = it
                                viewModel.updateRenderBackendUrl(it)
                            },
                            label = { Text("Render Web Service URL", color = TextSecondary) },
                            placeholder = { Text("https://your-service.onrender.com", color = TextSecondary.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricViolet,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("render_url_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Presets Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val defaultUrl = "https://photoframe-backend.onrender.com"
                                    localUrlText = defaultUrl
                                    viewModel.updateRenderBackendUrl(defaultUrl)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Default Cloud", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                            }

                            Button(
                                onClick = {
                                    val emuUrl = "http://10.0.2.2:8000"
                                    localUrlText = emuUrl
                                    viewModel.updateRenderBackendUrl(emuUrl)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Local Host (10.0.2.2)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Test & Deploy Guide
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.testBackendConnection() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                enabled = backendStatus !is BackendStatus.Connecting,
                                modifier = Modifier.weight(1f).testTag("test_backend_button")
                            ) {
                                if (backendStatus is BackendStatus.Connecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Testing...", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Connection", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Button(
                                onClick = { showDeployDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                modifier = Modifier.weight(1f).testTag("deploy_guide_button")
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deploy Guide", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Cloud Enhancement Toggle Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cloud DSLR Engine (क्लाउड इंजन)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Uses Render cloud server for OpenCV CLAHE & 50mm Prime lens sharpness",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Switch(
                                checked = useCloudEnhance,
                                onCheckedChange = { viewModel.setUseRenderCloudEnhance(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricViolet,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Built-in presets list
            item {
                SectionHeader(
                    title = "Built-In Extraction Presets",
                    subtitle = "Tap to apply as default timeline interval"
                )
            }

            items(PresetRepository.defaultPresets, key = { it.id }) { preset ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.applyPreset(preset) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = preset.intervalLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = ElectricViolet,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // About MG Shankar Tech
            item {
                SectionHeader(
                    title = "About PHOTO FRAME",
                    subtitle = "Developer & Brand Attribution"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(ElectricViolet),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "PHOTO FRAME",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Created by MG Shankar Tech",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Find the Perfect Moment. Frame It Forever.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "PHOTO FRAME is engineered for photographers, cinematographers, athletes, vloggers, and everyday creators to unlock ultra-high precision, millisecond-accurate photographic frames from video recordings with full offline privacy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SoftGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% Local Device Processing • Zero Cloud Uploads",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SoftGreen
                                )
                            )
                        }
                    }
                }
            }
        }

        // Render Deploy Guide Dialog
        if (showDeployDialog) {
            AlertDialog(
                onDismissRequest = { showDeployDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Render पर डिप्लॉय करने की विधि",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Render (render.com) पर बैकएंड को 1-क्लिक में फ्री में डिप्लॉय करें:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeonCyan
                        )

                        Text(
                            text = "1. अपने कोड को GitHub पर पुश करें।\n" +
                                   "2. dashboard.render.com खोलें और 'New +' -> 'Blueprint' चुनें।\n" +
                                   "3. रिपॉजिटरी सेलेक्ट करें — Render स्वतः 'render.yaml' पहचान लेगा।\n" +
                                   "4. 'Apply' पर क्लिक करें। 2 मिनट में आपका बैकएंड लाइव हो जाएगा!\n" +
                                   "5. Render से मिला URL (e.g. https://your-app.onrender.com) यहाँ पेस्ट करके 'Test Connection' दबाएं।",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )

                        Text(
                            text = "💡 नोट: विस्तृत निर्देश और Docker फाइल प्रोजेक्ट की DEPLOY_ON_RENDER.md में उपलब्ध हैं।",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDeployDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text("समझ गया (Done)")
                    }
                },
                containerColor = DarkSurfaceCard,
                textContentColor = TextPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun HelpScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PhotoFrameTopBar(
            title = "Help & Principles",
            subtitle = "Video Frame Rate vs Milliseconds",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Why Frame Rate Determines True Precision",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "A user-selected time interval and a video's actual recorded frame rate are distinct:\n\n" +
                                    "• 30 FPS video = ~1 real frame every 33.33 milliseconds.\n" +
                                    "• 60 FPS video = ~1 real frame every 16.67 milliseconds.\n" +
                                    "• 120 FPS slow-motion = ~1 real frame every 8.33 milliseconds.\n" +
                                    "• 240 FPS ultra slow-mo = ~1 real frame every 4.16 milliseconds.\n\n" +
                                    "Requesting an extraction interval of 1 millisecond samples the nearest recorded physical frame. It cannot invent frames that were never exposed by the physical camera sensor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "How AI Quality Ranking Works",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SoftGreen
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "PHOTO FRAME analyzes every extracted frame using on-device computer vision algorithms:\n\n" +
                                    "1. Edge Sharpness: Discrete Laplacian gradient variance.\n" +
                                    "2. Exposure Balance: Histogram luminance distribution.\n" +
                                    "3. Contrast: Standard deviation of pixel luminosity.\n" +
                                    "4. Color Vibrancy: Mean HSV saturation.\n" +
                                    "5. Duplicate Detection: 64-bit dHash perceptual fingerprinting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BrandBadge()
                }
            }
        }
    }
}

private data class StatusDisplay(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val title: String,
    val desc: String
)

