package com.example.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VipContact
import com.example.ui.RingtoneViewModel
import com.example.ui.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LimitNoticeCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonViolet

@Composable
fun VipManagerScreen(
    uiState: UiState,
    viewModel: RingtoneViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vipContacts = uiState.vipContacts
    val maxLimit = 5

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedVipForAudioAssign by remember { mutableStateOf<VipContact?>(null) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            extractContactDetails(context, contactUri)?.let { (contactId, name, number) ->
                viewModel.addVipContact(contactId, name, number)
            }
        }
    }

    val audioPickerForVipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { audioUri ->
        selectedVipForAudioAssign?.let { vip ->
            if (audioUri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        audioUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val fileName = getFileName(context, audioUri) ?: "VIP Custom Ringtone"
                viewModel.assignAudioToVip(vip.id, fileName, audioUri.toString())
            }
        }
        selectedVipForAudioAssign = null
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
                title = "VIP Callers Manager",
                subtitle = "Assign specific ringtones to up to 5 VIP contacts",
                icon = Icons.Default.Star
            )
        }

        item {
            LimitNoticeCard(
                currentCount = vipContacts.size,
                maxLimit = maxLimit,
                itemLabel = "VIP contacts"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Priority Override Logic",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "On incoming calls, VIP contacts play their assigned track first. Non-VIP callers play tracks from the General Playlist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (vipContacts.size < maxLimit) {
                            try {
                                contactPickerLauncher.launch(null)
                            } catch (e: Exception) {
                                showAddDialog = true
                            }
                        }
                    },
                    enabled = vipContacts.size < maxLimit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        disabledContainerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pick Contact",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (vipContacts.size < maxLimit) {
                            showAddDialog = true
                        }
                    },
                    enabled = vipContacts.size < maxLimit,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual Entry", fontSize = 12.sp)
                }
            }
        }

        if (vipContacts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No VIP Contacts Added",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Use the buttons above to add VIP contacts and assign unique ringtones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(vipContacts, key = { it.id }) { vip ->
                VipContactCard(
                    vip = vip,
                    isPreviewing = uiState.previewingUri == vip.audioUriString,
                    onAssignAudio = {
                        selectedVipForAudioAssign = vip
                        audioPickerForVipLauncher.launch(arrayOf("audio/*"))
                    },
                    onAssignPresetRingtone = {
                        assignSampleSystemRingtone(context, viewModel, vip)
                    },
                    onTogglePreview = {
                        vip.audioUriString?.let { uri ->
                            viewModel.togglePreviewAudio(context, uri)
                        }
                    },
                    onRemoveAudio = {
                        viewModel.removeAudioFromVip(vip.id)
                    },
                    onDeleteVip = {
                        viewModel.deleteVipContact(vip)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        AddVipContactDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, number ->
                showAddDialog = false
                viewModel.addVipContact("manual_${System.currentTimeMillis()}", name, number)
            }
        )
    }
}

@Composable
fun VipContactCard(
    vip: VipContact,
    isPreviewing: Boolean,
    onAssignAudio: () -> Unit,
    onAssignPresetRingtone: () -> Unit,
    onTogglePreview: () -> Unit,
    onRemoveAudio: () -> Unit,
    onDeleteVip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vip.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NeonAmber
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = vip.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "VIP",
                                tint = NeonAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = vip.phoneNumber,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDeleteVip) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete VIP Contact",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = if (vip.audioUriString != null) NeonViolet.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (vip.audioUriString != null) NeonViolet else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = vip.audioTitle ?: "No Specific Track Assigned",
                                fontWeight = if (vip.audioUriString != null) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (vip.audioUriString != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            if (vip.audioUriString == null) {
                                Text(
                                    text = "Falls back to General Playlist",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (vip.audioUriString != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onTogglePreview,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isPreviewing) NeonGreen else NeonCyan.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Preview Audio",
                                    tint = if (isPreviewing) Color.Black else NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onRemoveAudio) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove assigned track",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAssignAudio,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select Audio File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAssignPresetRingtone,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Preset Tone", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AddVipContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, number: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add VIP Contact", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        onAdd(name.trim(), number.trim())
                    }
                },
                enabled = name.isNotBlank() && number.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
            ) {
                Text("Add VIP", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = DarkSurfaceVariant
    )
}

private fun extractContactDetails(context: Context, contactUri: Uri): Triple<String, String, String>? {
    var contactId = ""
    var name = ""
    var number = ""

    try {
        val cursor = context.contentResolver.query(contactUri, null, null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                if (idIndex != -1) contactId = c.getString(idIndex)
                if (nameIndex != -1) name = c.getString(nameIndex) ?: "VIP Contact"

                val hasPhone = if (hasPhoneIndex != -1) c.getInt(hasPhoneIndex) else 0
                if (hasPhone > 0) {
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { pc ->
                        if (pc.moveToFirst()) {
                            val numIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIndex != -1) {
                                number = pc.getString(numIndex)
                            }
                        }
                    }
                }
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
        Toast.makeText(context, "Permission Denied! Please allow contacts permission.", Toast.LENGTH_LONG).show()
        return null
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    return if (name.isNotBlank() && number.isNotBlank()) {
        Triple(contactId.ifBlank { "contact_${System.currentTimeMillis()}" }, name, number)
    } else null
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

private fun assignSampleSystemRingtone(context: Context, viewModel: RingtoneViewModel, vip: VipContact) {
    val ringtoneManager = RingtoneManager(context)
    ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
    val cursor = ringtoneManager.cursor
    if (cursor.moveToFirst()) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
        val uri = ringtoneManager.getRingtoneUri(cursor.position)
        viewModel.assignAudioToVip(vip.id, title, uri.toString())
    }
}
