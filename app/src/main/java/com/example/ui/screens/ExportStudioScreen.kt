package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExportConfig
import com.example.model.ExportFormat
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
import com.example.util.ImageExporter
import com.example.util.TimestampFormatter
import com.example.viewmodel.MainViewModel
import java.io.File

@Composable
fun ExportStudioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val exportConfig by viewModel.exportConfig.collectAsState()
    val selectedFrames = viewModel.getSelectedOrAllFrames()
    val statusMsg by viewModel.exportStatusMessage.collectAsState()
    val context = LocalContext.current

    var generatedZipFile by remember { mutableStateOf<File?>(null) }
    var generatedContactSheetFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PhotoFrameTopBar(
            title = "Export & Archive Studio",
            subtitle = "${selectedFrames.size} frames selected for output",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status banner if any
            if (statusMsg != null) {
                item {
                    Surface(
                        color = ElectricViolet.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusMsg ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // 1. FORMAT & QUALITY CONFIGURATION
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Output Image Format",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Format Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                Pair("JPEG", ExportFormat.JPEG),
                                Pair("PNG (Lossless)", ExportFormat.PNG),
                                Pair("WebP", ExportFormat.WEBP)
                            ).forEach { (label, format) ->
                                val isSelected = exportConfig.format == format
                                Surface(
                                    color = if (isSelected) ElectricViolet else DarkSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ElectricViolet else DarkBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable {
                                            viewModel.updateExportConfig(exportConfig.copy(format = format))
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (exportConfig.format != ExportFormat.PNG) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Compression Quality: ${exportConfig.quality}%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Slider(
                                value = exportConfig.quality.toFloat(),
                                onValueChange = {
                                    viewModel.updateExportConfig(exportConfig.copy(quality = it.toInt()))
                                },
                                valueRange = 60f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filename template notice
                        Text(
                            text = "Filename Pattern: PHOTO_FRAME_00-00-02-125.${exportConfig.format.name.lowercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }

            // 2. EXPORT ACTIONS
            item {
                SectionHeader(
                    title = "Export Destinations",
                    subtitle = "High-resolution output directly to your phone storage"
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Option A: Save to Android Device Gallery
                    ExportActionTile(
                        title = "Save to Device Gallery",
                        description = "Exports all selected frames in full resolution to Pictures/PhotoFrame",
                        icon = Icons.Default.PhotoLibrary,
                        iconTint = NeonCyan,
                        buttonLabel = "Export ${selectedFrames.size} Photos",
                        onAction = {
                            viewModel.exportSelectedFramesBatch {}
                        }
                    )

                    // Option B: Export ZIP Archive with Manifest
                    ExportActionTile(
                        title = "Download ZIP Archive",
                        description = "Bundles all frames + manifest.json & manifest.csv with exact timestamps",
                        icon = Icons.Default.Archive,
                        iconTint = ElectricViolet,
                        buttonLabel = if (generatedZipFile != null) "Share ZIP Archive" else "Create ZIP Package",
                        onAction = {
                            if (generatedZipFile != null) {
                                val shareIntent = ImageExporter.buildShareZipIntent(context, generatedZipFile!!)
                                context.startActivity(Intent.createChooser(shareIntent, "Share ZIP"))
                            } else {
                                viewModel.exportZipArchive { file ->
                                    generatedZipFile = file
                                }
                            }
                        }
                    )

                    // Option C: Generate Contact Sheet
                    ExportActionTile(
                        title = "Generate Contact Sheet",
                        description = "Stitches thumbnails into a single studio overview sheet with labels",
                        icon = Icons.Default.GridOn,
                        iconTint = SoftGreen,
                        buttonLabel = if (generatedContactSheetFile != null) "Share Contact Sheet" else "Create Contact Sheet",
                        onAction = {
                            if (generatedContactSheetFile != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        generatedContactSheetFile!!
                                    )
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Contact Sheet"))
                            } else {
                                viewModel.exportContactSheet { file ->
                                    generatedContactSheetFile = file
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExportActionTile(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iconTint),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (iconTint == SoftGreen || iconTint == NeonCyan) Color.Black else Color.White
                    )
                )
            }
        }
    }
}
