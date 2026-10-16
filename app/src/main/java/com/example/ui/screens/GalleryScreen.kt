package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.model.ExtractedFrame
import com.example.ui.components.PhotoFrameTopBar
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
import com.example.util.ImageExporter
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GalleryFilter
import com.example.viewmodel.GallerySort
import com.example.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val frames = viewModel.getFilteredFrames()
    val allFrames = viewModel.extractedFrames.collectAsState().value
    val selectedIds by viewModel.selectedFrameIds.collectAsState()
    val currentFilter by viewModel.galleryFilter.collectAsState()
    val currentSort by viewModel.gallerySort.collectAsState()

    val isAnalyzingSituation by viewModel.isAnalyzingSituation.collectAsState()
    val situationResult by viewModel.situationResult.collectAsState()
    val situationQuery by viewModel.situationQuery.collectAsState()

    var showSituationSpotter by remember { mutableStateOf(false) }
    var situationPromptInput by remember { mutableStateOf(situationQuery) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showSelectionActions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Top Bar
        PhotoFrameTopBar(
            title = "Extracted Frame Gallery",
            subtitle = "${frames.size} of ${allFrames.size} photos",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() },
            actions = {
                // AI Situation Search Toggle Button
                IconButton(
                    onClick = { showSituationSpotter = !showSituationSpotter },
                    modifier = Modifier.testTag("toggle_situation_spotter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Situation Spotter",
                        tint = if (showSituationSpotter || situationResult != null) NeonCyan else TextSecondary
                    )
                }

                // Sort Menu
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Frames",
                            tint = NeonCyan
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkSurfaceCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Timestamp (Earliest First)", color = TextPrimary) },
                            onClick = {
                                viewModel.setGallerySort(GallerySort.TIMESTAMP_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Timestamp (Latest First)", color = TextPrimary) },
                            onClick = {
                                viewModel.setGallerySort(GallerySort.TIMESTAMP_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AI Situation Match 🎯", color = NeonCyan) },
                            onClick = {
                                viewModel.setGallerySort(GallerySort.SITUATION_MATCH_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AI Best-Photo Score ★", color = SoftGreen) },
                            onClick = {
                                viewModel.setGallerySort(GallerySort.AI_SCORE_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest Sharpness", color = NeonCyan) },
                            onClick = {
                                viewModel.setGallerySort(GallerySort.SHARPNESS_DESC)
                                showSortMenu = false
                            }
                        )
                    }
                }

                // Selection toggle
                IconButton(
                    onClick = { showSelectionActions = !showSelectionActions },
                    modifier = Modifier.testTag("selection_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selection Tools",
                        tint = if (selectedIds.isNotEmpty()) ElectricViolet else TextSecondary
                    )
                }
            }
        )

        // 1. AI SITUATION SPOTTER PROMPT BAR & DRAWER
        AnimatedVisibility(visible = showSituationSpotter || situationResult != null) {
            Surface(
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Situation Spotter (सिचुएशन आधारित शॉट खोजें)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                showSituationSpotter = false
                                viewModel.clearSituationSearch()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "वीडियो में कोई भी सिचुएशन या मोमेंट डिस्क्राइब करें (उदा. 'कार का एक्सीडेंट शॉट', 'केक काटने की स्माइल', 'डांस का पीक जंप')",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Text Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = situationPromptInput,
                            onValueChange = { situationPromptInput = it },
                            placeholder = {
                                Text(
                                    "जैसे: 'कार का एक्सीडेंट मोमेंट शॉट निकालो'...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("situation_search_input")
                        )

                        Button(
                            onClick = { viewModel.searchSituation(situationPromptInput) },
                            enabled = !isAnalyzingSituation && situationPromptInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("run_situation_search_button")
                        ) {
                            if (isAnalyzingSituation) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Find Shot")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Example prompt suggestion chips
                    val examples = listOf(
                        "🚗 Car crash / accident moment",
                        "💃 Dance peak jump in air",
                        "🎂 Blowing candles smile",
                        "⚽ Goal kick celebration",
                        "💍 Ring exchange ceremony"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        examples.forEach { example ->
                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.clickable {
                                    situationPromptInput = example
                                    viewModel.searchSituation(example)
                                }
                            ) {
                                Text(
                                    text = example,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // WINNING SHOT RESULT CARD
                    if (situationResult != null) {
                        val res = situationResult!!
                        val winningFrame = allFrames.find { it.id == res.winningFrameId }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (winningFrame != null) "Exact Shot Found: Frame #${winningFrame.index + 1} (${winningFrame.timestampFormatted})" else "Situation Identified",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                        )
                                    }

                                    if (winningFrame != null) {
                                        Surface(
                                            color = SoftGreen,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${winningFrame.situationMatchScore}% MATCH",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = Color.Black
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = res.explanation,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextPrimary
                                )

                                if (winningFrame != null) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.openPhotoViewer(winningFrame) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View Exact Shot", style = MaterialTheme.typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.exportSingleFrame(winningFrame) {} },
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save This Shot", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. FILTER TABS
        val filterTabs = mutableListOf(
            Pair("All Frames", GalleryFilter.ALL)
        )
        if (situationResult != null) {
            filterTabs.add(Pair("AI Situation Match 🎯", GalleryFilter.SITUATION_MATCH))
        }
        filterTabs.addAll(
            listOf(
                Pair("Top Picks ★", GalleryFilter.TOP_PICKS),
                Pair("Sharpest", GalleryFilter.SHARPEST),
                Pair("Well Exposed", GalleryFilter.WELL_EXPOSED),
                Pair("Favorites ♥", GalleryFilter.FAVORITES),
                Pair("No Duplicates", GalleryFilter.NO_DUPLICATES)
            )
        )

        ScrollableTabRow(
            selectedTabIndex = filterTabs.indexOfFirst { it.second == currentFilter }.coerceAtLeast(0),
            containerColor = DarkSurface,
            contentColor = NeonCyan,
            edgePadding = 12.dp,
            divider = {}
        ) {
            filterTabs.forEach { (title, filter) ->
                val isSelected = currentFilter == filter
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setGalleryFilter(filter) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        )
                    }
                )
            }
        }

        // 3. SELECTION BAR (When open or items selected)
        if (showSelectionActions || selectedIds.isNotEmpty()) {
            Surface(
                color = DarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIds.size} Selected",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.selectAllFrames() }) {
                            Text("Select All", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.deselectAllFrames() }) {
                            Text("Deselect", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.invertFrameSelection() }) {
                            Text("Invert", style = MaterialTheme.typography.labelSmall)
                        }
                        if (selectedIds.isNotEmpty()) {
                            IconButton(onClick = { viewModel.deleteSelectedFrames() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. FRAME GRID
        if (frames.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No frames match this filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.setGalleryFilter(GalleryFilter.ALL) }) {
                        Text("Show All Frames", color = NeonCyan)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(frames, key = { it.id }) { frame ->
                    FrameGridCard(
                        frame = frame,
                        isSelected = frame.id in selectedIds,
                        onCardClick = { viewModel.openPhotoViewer(frame) },
                        onSelectToggle = { viewModel.toggleFrameSelection(frame.id) },
                        onFavoriteToggle = { viewModel.toggleFavorite(frame.id) }
                    )
                }
            }
        }

        // 5. BOTTOM ACTION BAR (Export, Compare, Share)
        Surface(
            color = DarkSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compare Button
                OutlinedButton(
                    onClick = { viewModel.launchComparisonStudio() },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("compare_frames_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Compare",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Share Button
                IconButton(
                    onClick = {
                        val toShare = viewModel.getSelectedOrAllFrames()
                        val shareIntent = ImageExporter.buildShareIntent(context, toShare)
                        if (shareIntent != null) {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Frames"))
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Share Frames",
                        tint = TextPrimary
                    )
                }

                // Export Button
                Button(
                    onClick = { viewModel.navigateTo(AppScreen.EXPORT_STUDIO) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("export_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedIds.isNotEmpty()) "Export (${selectedIds.size})" else "Export Photos",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun FrameGridCard(
    frame: ExtractedFrame,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else if (frame.situationMatchScore >= 80) 2.dp else 1.dp,
            if (isSelected) ElectricViolet else if (frame.situationMatchScore >= 80) SoftGreen else DarkBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("frame_card_${frame.index}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                // Thumbnail image
                val thumbFile = File(if (File(frame.thumbPath).exists()) frame.thumbPath else frame.imagePath)
                if (thumbFile.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(thumbFile)
                                .build()
                        ),
                        contentDescription = "Frame at ${frame.timestampFormatted}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // AI Quality Score Badge (Top-Left)
                if (frame.aiScore > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0F172A).copy(alpha = 0.85f),
                                        Color(0xFF1E293B).copy(alpha = 0.85f)
                                    )
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, if (frame.aiScore >= 80) SoftGreen else DarkBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (frame.aiScore >= 80) SoftGreen else NeonCyan,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${frame.aiScore}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (frame.aiScore >= 80) SoftGreen else TextPrimary
                                )
                            )
                        }
                    }
                }

                // AI Situation Match Badge (Bottom-Left)
                if (frame.situationMatchScore > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(
                                if (frame.situationMatchScore >= 80) SoftGreen else NeonCyan,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🎯 ${frame.situationMatchScore}% Match",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color.Black
                            )
                        )
                    }
                }

                // Selection Checkbox (Top-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricViolet else Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, if (isSelected) ElectricViolet else Color.White, CircleShape)
                        .clickable(onClick = onSelectToggle),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Duplicate Tag if flagged
                if (frame.isDuplicate) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(AmberAlert.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DUPLICATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            // Caption details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${frame.index + 1}  •  ${frame.timestampFormatted}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 10.sp
                        )
                    )
                    if (frame.situationMatchReason != null) {
                        Text(
                            text = frame.situationMatchReason,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = NeonCyan
                            ),
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (frame.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (frame.isFavorite) Color(0xFFF43F5E) else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
