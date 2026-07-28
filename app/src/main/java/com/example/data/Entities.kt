package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "general_tracks")
data class GeneralTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uriString: String,
    val durationMs: Long = 0,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vip_contacts")
data class VipContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: String,
    val name: String,
    val phoneNumber: String,
    val audioTitle: String? = null,
    val audioUriString: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val masterEnabled: Boolean = true,
    val playbackMode: String = "SEQUENTIAL", // "SEQUENTIAL" or "RANDOM"
    val lastPlayedIndex: Int = 0
)

@Entity(tableName = "call_logs")
data class CallLogItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callerName: String? = null,
    val isVip: Boolean = false,
    val ringtoneName: String,
    val timestamp: Long = System.currentTimeMillis()
)
