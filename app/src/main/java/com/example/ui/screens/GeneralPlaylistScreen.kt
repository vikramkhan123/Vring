package com.example.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeneralTrack
import com.example.ui.RingtoneViewModel
import com.example.ui.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LimitNoticeCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonViolet

@Composable
fun GeneralPlaylistScreen(
    uiState: UiState,
    viewModel: RingtoneViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracks = uiState.generalTracks
    val maxLimit = 10

    // Multi-audio document picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSlots = maxLimit - tracks.size
            if (remainingSlots <= 0) {
                // Limit already reached! Handled in VM as well.
                viewModel.addGeneralTrack("", "", 0) // Triggers error message in VM
            } else {
                val selectedList = uris.take(remainingSlots)
                if (uris.size > remainingSlots) {
                    // Let user know some were skipped due to limit
                    viewModel.addGeneralTrack("", "", 0)
                }
                selectedList.forEach { uri ->
                    val fileName = getFileName(context, uri) ?: "Audio Track ${tracks.size + 1}"
                    viewModel.addGeneralTrack(fileName, uri.toString(), 0)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "General Playlist",
                subtitle = "Audio files played for non-VIP incoming calls (Max 10)",
                icon = Icons.Default.LibraryMusic
            )
        }

        // Limit Indicator Card
        item {
            LimitNoticeCard(
                currentCount = tracks.size,
                maxLimit = maxLimit,
                itemLabel = "songs"
            )
        }

        // Playback Order Toggle Control Card
        item {
            PlaybackModeCard(
                currentMode = uiState.config.playbackMode,
                onModeChange = { viewModel.setPlaybackMode(it) }
            )
        }

        // Add Audio Button Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        audioPickerLauncher.launch("audio/*")
                    },
                    enabled = tracks.size < maxLimit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonViolet,
                        disabledContainerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Audio (.mp3/.wav)", fontWeight = FontWeight.Bold)
                }

                if (tracks.size < maxLimit) {
                    OutlinedButton(
                        onClick = {
                            addPresetSystemRingtones(context, viewModel, tracks.size)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Add Presets", fontSize = 12.sp)
                    }
                }
            }
        }

        // Tracks List or Empty State
        if (tracks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "General Playlist is Empty",
                    description = "Select up to 10 audio files (.mp3, .wav) from device storage or add sample ringtone presets to get started.",
                    icon = Icons.Default.MusicNote,
                    actionLabel = "Add Sample Ringtone Presets",
                    onAction = {
                        addPresetSystemRingtones(context, viewModel, 0)
                    }
                )
            }
        } else {
            itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
                GeneralTrackRow(
                    index = index + 1,
                    track = track,
                    isPreviewing = uiState.previewingUri == track.uriString,
                    onTogglePreview = { viewModel.togglePreviewAudio(context, track.uriString) },
                    onDelete = { viewModel.deleteGeneralTrack(track) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PlaybackModeCard(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dynamic Playback Order",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Choose how songs cycle for consecutive incoming calls from non-VIP numbers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sequential Option
                Surface(
                    onClick = { onModeChange("SEQUENTIAL") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentMode == "SEQUENTIAL") NeonViolet.copy(alpha = 0.25f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (currentMode == "SEQUENTIAL") NeonViolet else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = if (currentMode == "SEQUENTIAL") NeonViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sequential (1,2,3..)",
                            fontSize = 12.sp,
                            fontWeight = if (currentMode == "SEQUENTIAL") FontWeight.Bold else FontWeight.Normal,
                            color = if (currentMode == "SEQUENTIAL") NeonViolet else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Random Option
                Surface(
                    onClick = { onModeChange("RANDOM") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentMode == "RANDOM") NeonCyan.copy(alpha = 0.25f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (currentMode == "RANDOM") NeonCyan else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = if (currentMode == "RANDOM") NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Random Order",
                            fontSize = 12.sp,
                            fontWeight = if (currentMode == "RANDOM") FontWeight.Bold else FontWeight.Normal,
                            color = if (currentMode == "RANDOM") NeonCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralTrackRow(
    index: Int,
    track: GeneralTrack,
    isPreviewing: Boolean,
    onTogglePreview: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPreviewing) NeonGreen.copy(alpha = 0.2f) else NeonViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$index",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isPreviewing) NeonGreen else NeonViolet
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (isPreviewing) "Playing Preview..." else "Audio File (.mp3 / .wav)",
                        fontSize = 11.sp,
                        color = if (isPreviewing) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePreview,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPreviewing) NeonGreen else NeonCyan.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Preview Audio",
                        tint = if (isPreviewing) Color.Black else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Track",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

private fun addPresetSystemRingtones(context: Context, viewModel: RingtoneViewModel, currentCount: Int) {
    val ringtoneManager = RingtoneManager(context)
    ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
    val cursor = ringtoneManager.cursor
    var countAdded = 0
    val maxToAdd = 10 - currentCount

    while (cursor.moveToNext() && countAdded < maxToAdd && countAdded < 3) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
        val uri = ringtoneManager.getRingtoneUri(cursor.position)
        viewModel.addGeneralTrack(title, uri.toString(), 0)
        countAdded++
    }
}
