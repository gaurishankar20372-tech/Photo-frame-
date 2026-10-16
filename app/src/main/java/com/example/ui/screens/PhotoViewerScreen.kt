package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DslrProfile
import com.example.util.ImageExporter
import com.example.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoViewerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeFrame by viewModel.activeViewerFrame.collectAsState()
    val adjustments by viewModel.viewerAdjustments.collectAsState()
    val statusMsg by viewModel.exportStatusMessage.collectAsState()
    val context = LocalContext.current

    if (activeFrame == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No frame selected", color = TextSecondary)
        }
        return
    }

    val frame = activeFrame!!

    // Zoom & Pan transformation state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var showAdjustmentsSheet by remember { mutableStateOf(false) }

    // ColorFilter for live non-destructive DSLR preview
    val colorFilter = remember(adjustments) {
        if (!adjustments.dslrMasterEnhanceEnabled || adjustments.dslrProfile == DslrProfile.NONE) {
            val s = adjustments.contrast
            val t = (1f - s) * 128f + (adjustments.brightness * 2.55f)
            val sat = adjustments.saturation
            val lr = 0.213f * (1f - sat)
            val lg = 0.715f * (1f - sat)
            val lb = 0.072f * (1f - sat)

            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        (lr + sat) * s, lg * s, lb * s, 0f, t,
                        lr * s, (lg + sat) * s, lb * s, 0f, t,
                        lr * s, lg * s, (lb + sat) * s, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else {
            val contrastFactor = 1.0f + adjustments.microClarity * 0.45f
            val offset = (1f - contrastFactor) * 128f + (adjustments.shadowLift * 30f) + (adjustments.brightness * 2.55f)
            val sat = when (adjustments.dslrProfile) {
                DslrProfile.LEICA_MONOCHROME -> 0.0f
                DslrProfile.STUDIO_VIVID -> 1.35f * adjustments.saturation
                DslrProfile.CINEMATIC_TEAL_ORANGE -> 1.20f * adjustments.saturation
                DslrProfile.PORTRAIT_GLOW -> 1.10f * adjustments.saturation
                else -> 1.15f * adjustments.saturation
            }
            val lr = 0.213f * (1f - sat)
            val lg = 0.715f * (1f - sat)
            val lb = 0.072f * (1f - sat)
            val s = contrastFactor

            val rBoost = if (adjustments.dslrProfile == DslrProfile.CINEMATIC_TEAL_ORANGE) 8f else 3f
            val bBoost = if (adjustments.dslrProfile == DslrProfile.CINEMATIC_TEAL_ORANGE) -4f else 4f

            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        (lr + sat) * s, lg * s, lb * s, 0f, offset + rBoost,
                        lr * s, (lg + sat) * s, lb * s, 0f, offset,
                        lr * s, lg * s, (lb + sat) * s, 0f, offset + bBoost,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // TOP BAR
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Frame #${frame.index + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (adjustments.dslrMasterEnhanceEnabled) SoftGreen else DarkSurfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (adjustments.dslrMasterEnhanceEnabled) "DSLR PRO" else "RAW VIDEO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = if (adjustments.dslrMasterEnhanceEnabled) Color.Black else TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${frame.timestampFormatted}  •  ${frame.width}×${frame.height}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.testTag("viewer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Gallery",
                        tint = Color.White
                    )
                }
            },
            actions = {
                // Split Compare Before/After Button
                IconButton(onClick = {
                    viewModel.updateAdjustments(adjustments.copy(showSplitPreview = !adjustments.showSplitPreview))
                }) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Compare Before / After",
                        tint = if (adjustments.showSplitPreview) NeonCyan else Color.White
                    )
                }
                // Favorite
                IconButton(onClick = { viewModel.toggleFavorite(frame.id) }) {
                    Icon(
                        imageVector = if (frame.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (frame.isFavorite) Color(0xFFF43F5E) else Color.White
                    )
                }
                // Share
                IconButton(onClick = {
                    val shareIntent = ImageExporter.buildShareIntent(context, listOf(frame))
                    if (shareIntent != null) {
                        context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
                // Quick Save
                IconButton(onClick = { viewModel.exportSingleFrame(frame) {} }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to Gallery",
                        tint = NeonCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.85f)
            )
        )

        // Status Toast Message
        if (statusMsg != null) {
            Surface(
                color = ElectricViolet.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMsg ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                )
            }
        }

        // MAIN IMAGE PREVIEW WITH PINCH-TO-ZOOM & SPLIT VIEW
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val file = File(frame.imagePath)
            if (file.exists()) {
                if (adjustments.showSplitPreview) {
                    // BEFORE / AFTER SPLIT COMPARISON VIEW
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left: Original Flat Video Frame
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(file)
                                        .build()
                                ),
                                contentDescription = "Original Flat Video Frame",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                                    .rotate(adjustments.rotation.toFloat())
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "RAW VIDEO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Split Divider Line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(NeonCyan)
                        )

                        // Right: DSLR Enhanced Frame
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(file)
                                        .build()
                                ),
                                contentDescription = "DSLR Enhanced Photograph",
                                contentScale = ContentScale.Fit,
                                colorFilter = colorFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                                    .rotate(adjustments.rotation.toFloat())
                            )
                            Surface(
                                color = SoftGreen.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "DSLR MASTER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Full Screen View
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .build()
                        ),
                        contentDescription = "High-Res Photograph",
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .rotate(adjustments.rotation.toFloat())
                    )
                }
            }

            // Quality Badge (Bottom-Left)
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = SoftGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DSLR Clarity: ${frame.sharpnessScore.toInt()}%  •  32-Bit ARGB",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // BOTTOM DSLR STUDIO ENHANCEMENT PANEL
        Surface(
            color = DarkSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (showAdjustmentsSheet) {
                    // 1. MASTER DSLR SWITCH & PROFILES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SoftGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DSLR Master Enhance (डीएसएलआर ऑप्टिक्स)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Switch(
                            checked = adjustments.dslrMasterEnhanceEnabled,
                            onCheckedChange = {
                                viewModel.updateAdjustments(adjustments.copy(dslrMasterEnhanceEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SoftGreen),
                            modifier = Modifier.testTag("dslr_master_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DSLR Profile Selection Chips
                    val profiles = listOf(
                        Pair("📷 DSLR Prime", DslrProfile.DSLR_PRIME),
                        Pair("🏎️ Vivid Action", DslrProfile.STUDIO_VIVID),
                        Pair("🎬 Cinematic", DslrProfile.CINEMATIC_TEAL_ORANGE),
                        Pair("👤 Portrait", DslrProfile.PORTRAIT_GLOW),
                        Pair("🎞️ Leica B&W", DslrProfile.LEICA_MONOCHROME)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        profiles.forEach { (label, profile) ->
                            val isCur = adjustments.dslrProfile == profile
                            FilterChip(
                                selected = isCur,
                                onClick = {
                                    viewModel.updateAdjustments(
                                        adjustments.copy(
                                            dslrProfile = profile,
                                            dslrMasterEnhanceEnabled = true
                                        )
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sliders
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Clarity (Micro-Contrast)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Micro-Clarity",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(85.dp)
                            )
                            Slider(
                                value = adjustments.microClarity,
                                onValueChange = { viewModel.updateAdjustments(adjustments.copy(microClarity = it)) },
                                valueRange = 0.0f..0.8f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Shadow Recovery (Dynamic Range)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Shadow Lift",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(85.dp)
                            )
                            Slider(
                                value = adjustments.shadowLift,
                                onValueChange = { viewModel.updateAdjustments(adjustments.copy(shadowLift = it)) },
                                valueRange = 0.0f..0.6f,
                                colors = SliderDefaults.colors(thumbColor = SoftGreen, activeTrackColor = SoftGreen),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Sharpness Boost Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Optical Unsharp Mask Boost",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Switch(
                                checked = adjustments.sharpnessBoost,
                                onCheckedChange = {
                                    viewModel.updateAdjustments(adjustments.copy(sharpnessBoost = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonCyan)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                viewModel.updateAdjustments(adjustments.copy(rotation = (adjustments.rotation + 90) % 360))
                            }) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90", tint = NeonCyan)
                            }

                            IconButton(onClick = { viewModel.resetAdjustments() }) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Adjustments", tint = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Navigation Prev / Adjust / Next row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Frame
                    IconButton(
                        onClick = { viewModel.navigateViewerPrev() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("viewer_prev_frame")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Frame",
                            tint = Color.White
                        )
                    }

                    // Toggle DSLR Studio
                    Surface(
                        color = if (showAdjustmentsSheet) SoftGreen else DarkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { showAdjustmentsSheet = !showAdjustmentsSheet }
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (showAdjustmentsSheet) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAdjustmentsSheet) "Hide DSLR Tools" else "DSLR Photography Studio",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (showAdjustmentsSheet) Color.Black else Color.White
                                )
                            )
                        }
                    }

                    // Next Frame
                    IconButton(
                        onClick = { viewModel.navigateViewerNext() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("viewer_next_frame")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Frame",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
