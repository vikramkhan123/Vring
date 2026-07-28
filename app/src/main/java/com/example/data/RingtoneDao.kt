package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RingtoneDao {

    // General Tracks (Max 10)
    @Query("SELECT * FROM general_tracks ORDER BY position ASC, id ASC")
    fun getGeneralTracksFlow(): Flow<List<GeneralTrack>>

    @Query("SELECT * FROM general_tracks ORDER BY position ASC, id ASC")
    suspend fun getGeneralTracksSync(): List<GeneralTrack>

    @Query("SELECT COUNT(*) FROM general_tracks")
    suspend fun getGeneralTrackCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneralTrack(track: GeneralTrack): Long

    @Delete
    suspend fun deleteGeneralTrack(track: GeneralTrack)

    @Query("DELETE FROM general_tracks WHERE id = :id")
    suspend fun deleteGeneralTrackById(id: Long)

    // VIP Contacts (Max 5)
    @Query("SELECT * FROM vip_contacts ORDER BY name ASC")
    fun getVipContactsFlow(): Flow<List<VipContact>>

    @Query("SELECT * FROM vip_contacts ORDER BY name ASC")
    suspend fun getVipContactsSync(): List<VipContact>

    @Query("SELECT COUNT(*) FROM vip_contacts")
    suspend fun getVipContactCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVipContact(contact: VipContact): Long

    @Update
    suspend fun updateVipContact(contact: VipContact)

    @Delete
    suspend fun deleteVipContact(contact: VipContact)

    @Query("DELETE FROM vip_contacts WHERE id = :id")
    suspend fun deleteVipContactById(id: Long)

    // Config
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun getConfigFlow(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfigSync(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AppConfig)

    // Call Logs
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT 30")
    fun getCallLogsFlow(): Flow<List<CallLogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogItem)

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()
}
