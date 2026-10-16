package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExtractionMode
import com.example.model.RoundingStrategy
import com.example.ui.components.PhotoFrameTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberAlert
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
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrecisionEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val metadata by viewModel.videoMetadata.collectAsState()
    val config by viewModel.extractionConfig.collectAsState()
    val totalEstimatedFrames by viewModel.totalEstimatedFrames.collectAsState()

    if (metadata == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No video loaded", color = TextSecondary)
        }
        return
    }

    val meta = metadata!!
    val totalDurationUs = meta.durationMs * 1000L

    var customIntervalInput by remember {
        mutableStateOf("${config.intervalUs / 1000L}")
    }

    var selectedIntervalUnit by remember { mutableStateOf("ms") } // ms, us, sec

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PhotoFrameTopBar(
            title = "Precision Timeline Editor",
            subtitle = "Millisecond Extraction Studio",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. DUAL-HANDLE TIMELINE RANGE SLIDER
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extraction Range",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            TextButton(
                                onClick = {
                                    viewModel.setStartTimestampUs(0L)
                                    viewModel.setEndTimestampUs(totalDurationUs)
                                },
                                modifier = Modifier.testTag("reset_range_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Full Video",
                                    style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Range Slider
                        val startFraction = if (totalDurationUs > 0) {
                            (config.startTimestampUs.toFloat() / totalDurationUs).coerceIn(0f, 1f)
                        } else 0f
                        val endFraction = if (totalDurationUs > 0) {
                            (config.endTimestampUs.toFloat() / totalDurationUs).coerceIn(startFraction, 1f)
                        } else 1f

                        RangeSlider(
                            value = startFraction..endFraction,
                            onValueChange = { range ->
                                val newStart = (range.start * totalDurationUs).toLong()
                                val newEnd = (range.endInclusive * totalDurationUs).toLong()
                                viewModel.setStartTimestampUs(newStart)
                                viewModel.setEndTimestampUs(newEnd)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("timeline_range_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Segmented Start and End inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TimecodeInputField(
                                label = "START POINT",
                                timestampUs = config.startTimestampUs,
                                onUpdateUs = { viewModel.setStartTimestampUs(it) },
                                modifier = Modifier.weight(1f)
                            )
                            TimecodeInputField(
                                label = "END POINT",
                                timestampUs = config.endTimestampUs,
                                onUpdateUs = { viewModel.setEndTimestampUs(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. EXTRACTION INTERVAL PRESETS
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Timestamp Interval Presets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Select high-speed burst, standard framerate, or multi-second pacing",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val presets = listOf(
                            Pair("1 ms", 1_000L),
                            Pair("5 ms", 5_000L),
                            Pair("10 ms", 10_000L),
                            Pair("16.7 ms (60fps)", 16_667L),
                            Pair("20 ms", 20_000L),
                            Pair("25 ms", 25_000L),
                            Pair("33.3 ms (30fps)", 33_333L),
                            Pair("50 ms", 50_000L),
                            Pair("100 ms", 100_000L),
                            Pair("250 ms", 250_000L),
                            Pair("500 ms", 500_000L),
                            Pair("1.0 sec", 1_000_000L),
                            Pair("2.0 sec", 2_000_000L),
                            Pair("5.0 sec", 5_000_000L)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (label, us) ->
                                val isSelected = config.intervalUs == us
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setIntervalUs(us)
                                        customIntervalInput = "${us / 1000L}"
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricViolet,
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = TextPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) ElectricViolet else DarkBorder
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Numeric Interval Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customIntervalInput,
                                onValueChange = { newVal ->
                                    val digitsOnly = newVal.filter { it.isDigit() }
                                    customIntervalInput = digitsOnly
                                    val num = digitsOnly.toLongOrNull() ?: 1L
                                    val finalUs = when (selectedIntervalUnit) {
                                        "us" -> num
                                        "sec" -> num * 1_000_000L
                                        else -> num * 1000L // ms
                                    }
                                    viewModel.setIntervalUs(finalUs)
                                },
                                label = { Text("Custom Interval") },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_interval_input")
                            )

                            // Unit Selector (ms / us / sec)
                            Row(
                                modifier = Modifier
                                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                listOf("ms", "us", "sec").forEach { unit ->
                                    val isCur = selectedIntervalUnit == unit
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCur) ElectricViolet else Color.Transparent)
                                            .clickable {
                                                selectedIntervalUnit = unit
                                                val num = customIntervalInput.toLongOrNull() ?: 1L
                                                val finalUs = when (unit) {
                                                    "us" -> num
                                                    "sec" -> num * 1_000_000L
                                                    else -> num * 1000L
                                                }
                                                viewModel.setIntervalUs(finalUs)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = unit,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isCur) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. ADVANCED CONTROLS & DUPLICATE DETECTION
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Engine Settings & Filters",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // AI Quality Ranking Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Best-Photo Ranking",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Compute Laplacian sharpness, exposure, and contrast scores",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = config.runAiQualityRanking,
                                onCheckedChange = { viewModel.toggleAiRanking(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SoftGreen
                                ),
                                modifier = Modifier.testTag("ai_ranking_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Skip Duplicate Frames
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Skip Identical Duplicates",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Use 64-bit dHash to discard consecutive repeated frames",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = config.skipDuplicates,
                                onCheckedChange = { viewModel.toggleSkipDuplicates(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricViolet
                                ),
                                modifier = Modifier.testTag("skip_duplicates_switch")
                            )
                        }
                    }
                }
            }

            // 4. IMPORTANT TECHNICAL PRINCIPLE BANNER
            item {
                Surface(
                    color = NeonCyan.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Technical Principle: Video FPS vs. Milliseconds",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A ${String.format("%.0f", meta.fps)} FPS video contains approximately one frame every ${String.format("%.1f", 1000f / meta.fps)} ms. Requesting 1 ms intervals accesses the nearest recorded presentation frame without fabricating artificial intermediate frames.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 5. ESTIMATED FRAME COUNT & SAFETY NOTICE
            item {
                val isHighCount = totalEstimatedFrames > 300
                Surface(
                    color = if (isHighCount) AmberAlert.copy(alpha = 0.12f) else DarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isHighCount) AmberAlert.copy(alpha = 0.4f) else DarkBorder
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isHighCount) Icons.Default.Warning else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isHighCount) AmberAlert else NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Estimated Frames: ~$totalEstimatedFrames photos",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHighCount) AmberAlert else TextPrimary
                                )
                            )
                            Text(
                                text = if (isHighCount) {
                                    "High frame count will be capped at 300 to protect device memory."
                                } else {
                                    "Safe range within memory buffer limits."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 6. EXTRACTION TRIGGER BUTTON
            item {
                Button(
                    onClick = { viewModel.startExtraction() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_precision_extraction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Frame Extraction",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun TimecodeInputField(
    label: String,
    timestampUs: Long,
    onUpdateUs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawText by remember(timestampUs) {
        mutableStateOf(TimestampFormatter.formatUsToHhMmSsMmm(timestampUs))
    }

    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = rawText,
                onValueChange = { newVal ->
                    rawText = newVal
                    val parsed = TimestampFormatter.parseTimestampToUs(newVal)
                    if (parsed != null) {
                        onUpdateUs(parsed)
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
