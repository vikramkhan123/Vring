package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.CallLogItem
import com.example.ui.RingtoneViewModel
import com.example.ui.UiState
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: UiState,
    viewModel: RingtoneViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var simInputNumber by remember { mutableStateOf("+15551234567") }

    val requiredPermissions = remember {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissions.toTypedArray()
    }

    var permissionsGranted by remember {
        mutableStateOf(checkAllPermissions(context, requiredPermissions))
    }

    var batteryOptimizationIgnored by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (!permissionsGranted) {
            Toast.makeText(context, "All permissions are mandatory for the app to work!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionsGranted = checkAllPermissions(context, requiredPermissions)
        batteryOptimizationIgnored = checkBatteryOptimization(context)
        
        if (!permissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else if (!batteryOptimizationIgnored) {
            requestBatteryOptimizationExemption(context)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeroHeaderCard(
                    masterEnabled = uiState.config.masterEnabled,
                    onToggleMaster = { viewModel.toggleMasterEnabled(it) }
                )
            }

            // NEW: Background Protection Guide Card for User
            item {
                BackgroundProtectionCard(
                    onOpenSettings = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }

            item {
                OverviewSummaryGrid(
                    generalCount = uiState.generalTracks.size,
                    vipCount = uiState.vipContacts.size,
                    playbackMode = uiState.config.playbackMode
                )
            }

            item {
                CallSimulatorCard(
                    isSimulating = uiState.isSimulatingCall,
                    simulatedNumber = uiState.simulatedNumber,
                    simInputNumber = simInputNumber,
                    onNumberChange = { simInputNumber = it },
                    vipContacts = uiState.vipContacts,
                    onSimulateCall = { number ->
                        viewModel.simulateIncomingCall(context, number)
                    },
                    onStopCall = {
                        viewModel.stopSimulatedCall(context)
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Intercepted Calls Log",
                        subtitle = "History of custom ringtones played",
                        icon = Icons.Default.SwapCalls,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.callLogs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCallLogs() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear logs",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (uiState.callLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No incoming calls intercepted yet.\nUse the simulator above to test custom ringtones!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(uiState.callLogs, key = { it.id }) { log ->
                    CallLogRow(log = log)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ENGLISH MANDATORY LOCK SCREEN
        if (!permissionsGranted || !batteryOptimizationIgnored) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.90f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Mandatory Action Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (!permissionsGranted) {
                                "To detect incoming calls and play custom ringtones, you must grant Phone, Contacts, and Audio permissions."
                            } else {
                                "To prevent the Android system from killing this app in the background, you must disable Battery Optimization (set to 'Unrestricted')."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = {
                                permissionsGranted = checkAllPermissions(context, requiredPermissions)
                                batteryOptimizationIgnored = checkBatteryOptimization(context)

                                if (!permissionsGranted) {
                                    permissionLauncher.launch(requiredPermissions)
                                } else if (!batteryOptimizationIgnored) {
                                    requestBatteryOptimizationExemption(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (!permissionsGranted) "Grant Permissions" else "Disable Battery Optimization",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// NEW: Background Protection Guide Card Composable
@Composable
fun BackgroundProtectionCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep App Alive in Background",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "To ensure ringtones play even after clearing recents, enable 'Autostart' and set Battery to 'Unrestricted' in App Info.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Settings", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun checkAllPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
}

private fun requestBatteryOptimizationExemption(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

@Composable
fun HeroHeaderCard(masterEnabled: Boolean, onToggleMaster: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (masterEnabled) listOf(Color(0xFF3B0764), Color(0xFF1E1B4B)) else listOf(Color(0xFF1F2937), Color(0xFF111827))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (masterEnabled) NeonViolet else Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dynamic Ringtone",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Call Interceptor & Switcher",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                    StatusBadge(active = masterEnabled)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (masterEnabled) NeonGreen else Color.LightGray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Master Interceptor",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (masterEnabled) "Mutes system ring & plays custom audio" else "App paused (system ringtone plays)",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                        Switch(
                            checked = masterEnabled,
                            onCheckedChange = onToggleMaster,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewSummaryGrid(generalCount: Int, vipCount: Int, playbackMode: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$generalCount/10",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (generalCount >= 10) NeonPink else NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "General Songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (playbackMode == "SEQUENTIAL") "Sequential" else "Random",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$vipCount/5",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (vipCount >= 5) NeonPink else NeonAmber
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VIP Contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$vipCount Assigned",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CallSimulatorCard(
    isSimulating: Boolean,
    simulatedNumber: String,
    simInputNumber: String,
    onNumberChange: (String) -> Unit,
    vipContacts: List<com.example.data.VipContact>,
    onSimulateCall: (String) -> Unit,
    onStopCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonPink.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Call Simulator & Tester",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isSimulating) {
                    StatusBadge(active = true, activeText = "RINGING")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Simulate an incoming call directly inside the app to test instantaneous ringtone switching and muting logic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isSimulating) {
                Surface(
                    color = Color(0xFF831843),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Simulated Incoming Call Ringing...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "From: $simulatedNumber",
                            fontSize = 13.sp,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onStopCall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CallEnd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hang Up / Stop Audio")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = simInputNumber,
                    onValueChange = onNumberChange,
                    label = { Text("Incoming Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSimulateCall(simInputNumber) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simulate Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (vipContacts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val vip = vipContacts.first()
                                onSimulateCall(vip.phoneNumber)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test VIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogRow(log: CallLogItem) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp)
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
                        .background(if (log.isVip) NeonAmber.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.isVip) Icons.Default.Star else Icons.Default.Call,
                        contentDescription = null,
                        tint = if (log.isVip) NeonAmber else NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.callerName ?: log.phoneNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (log.isVip) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VIP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonAmber,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Played: ${log.ringtoneName}",
                        fontSize = 12.sp,
                        color = NeonViolet
                    )
                }
            }
            Text(
                text = formattedTime,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
