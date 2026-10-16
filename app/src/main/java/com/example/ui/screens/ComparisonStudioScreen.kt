package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatChip
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import java.io.File

@Composable
fun ComparisonStudioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val framesToCompare by viewModel.comparisonFrames.collectAsState()
    val allFrames by viewModel.extractedFrames.collectAsState()
    var winnerFrameId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PhotoFrameTopBar(
            title = "Frame Comparison Studio",
            subtitle = "Side-by-Side Quality Inspection",
            canNavigateBack = true,
            onNavigateBack = { viewModel.navigateBack() }
        )

        if (framesToCompare.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select at least 2 frames to compare", color = TextSecondary)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = "Candidate Frames (${framesToCompare.size})",
                    subtitle = "Compare micro-expressions, sharpness, and motion clarity"
                )
            }

            // Side-by-Side Split Cards (Display 2 columns or stacked depending on count)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    framesToCompare.take(2).forEach { frame ->
                        val isWinner = winnerFrameId == frame.id
                        ComparisonFrameCard(
                            frame = frame,
                            isWinner = isWinner,
                            onPickWinner = {
                                winnerFrameId = frame.id
                                viewModel.toggleFavorite(frame.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (framesToCompare.size > 2) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        framesToCompare.drop(2).take(2).forEach { frame ->
                            val isWinner = winnerFrameId == frame.id
                            ComparisonFrameCard(
                                frame = frame,
                                isWinner = isWinner,
                                onPickWinner = {
                                    winnerFrameId = frame.id
                                    viewModel.toggleFavorite(frame.id)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Metric Comparison Table
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Metric Telemetry Comparison",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        framesToCompare.forEach { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${f.index + 1} (${f.timestampFormatted})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (f.id == winnerFrameId) SoftGreen else TextPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Sharp: ${f.sharpnessScore.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "Score: ${f.aiScore}★",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SoftGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filmstrip of neighboring frames
            item {
                SectionHeader(
                    title = "Neighboring Timeline Frames",
                    subtitle = "Tap to swap candidates into comparison"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allFrames, key = { it.id }) { frame ->
                        val isCandidate = framesToCompare.any { it.id == frame.id }
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 75.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    if (isCandidate) 2.dp else 1.dp,
                                    if (isCandidate) ElectricViolet else DarkBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    // Swap candidate
                                    val newCandidates = if (isCandidate) {
                                        framesToCompare.filter { it.id != frame.id }
                                    } else {
                                        (framesToCompare + frame).take(4)
                                    }
                                    viewModel.launchComparisonStudio(newCandidates)
                                }
                        ) {
                            val thumbFile = File(if (File(frame.thumbPath).exists()) frame.thumbPath else frame.imagePath)
                            if (thumbFile.exists()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(thumbFile)
                                            .build()
                                    ),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = frame.timestampFormatted.takeLast(6),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonFrameCard(
    frame: ExtractedFrame,
    isWinner: Boolean,
    onPickWinner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            if (isWinner) 2.dp else 1.dp,
            if (isWinner) SoftGreen else DarkBorder
        ),
        modifier = modifier
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                val file = File(frame.imagePath)
                if (file.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .build()
                        ),
                        contentDescription = "Compare frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isWinner) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(SoftGreen, CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Winning Frame",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Frame #${frame.index + 1}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan
                )
                Text(
                    text = frame.timestampFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Score: ${frame.aiScore}★",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SoftGreen
                        )
                    )
                    Text(
                        text = "Sharp: ${frame.sharpnessScore.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onPickWinner,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWinner) SoftGreen else DarkSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("pick_winner_frame_${frame.index}")
                ) {
                    Text(
                        text = if (isWinner) "Best Pick ✓" else "Select As Best",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isWinner) Color.Black else TextPrimary
                        )
                    )
                }
            }
        }
    }
}
