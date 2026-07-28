package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class OperationResult {
    data class Success(val message: String? = null) : OperationResult()
    data class Error(val reason: String) : OperationResult()
}

class RingtoneRepository(private val dao: RingtoneDao) {

    val generalTracks: Flow<List<GeneralTrack>> = dao.getGeneralTracksFlow()
    val vipContacts: Flow<List<VipContact>> = dao.getVipContactsFlow()
    val config: Flow<AppConfig> = dao.getConfigFlow().map { it ?: AppConfig() }
    val callLogs: Flow<List<CallLogItem>> = dao.getCallLogsFlow()

    suspend fun getGeneralTracksSync(): List<GeneralTrack> = dao.getGeneralTracksSync()
    suspend fun getVipContactsSync(): List<VipContact> = dao.getVipContactsSync()
    suspend fun getConfigSync(): AppConfig = dao.getConfigSync() ?: AppConfig()

    suspend fun addGeneralTrack(title: String, uriString: String, durationMs: Long): OperationResult {
        val currentCount = dao.getGeneralTrackCount()
        if (currentCount >= 10) {
            return OperationResult.Error("Maximum limit reached. You can add up to 10 songs in the General Playlist.")
        }
        val track = GeneralTrack(
            title = title,
            uriString = uriString,
            durationMs = durationMs,
            position = currentCount
        )
        dao.insertGeneralTrack(track)
        return OperationResult.Success("Song added to General Playlist.")
    }

    suspend fun deleteGeneralTrack(track: GeneralTrack) {
        dao.deleteGeneralTrack(track)
    }

    suspend fun addVipContact(contactId: String, name: String, phoneNumber: String): OperationResult {
        val currentCount = dao.getVipContactCount()
        if (currentCount >= 5) {
            return OperationResult.Error("Maximum limit reached. You can assign up to 5 VIP contacts.")
        }
        // Check if contact already exists
        val existing = dao.getVipContactsSync().find { 
            it.contactId == contactId || it.phoneNumber == phoneNumber 
        }
        if (existing != null) {
            return OperationResult.Error("Contact is already in VIP list.")
        }
        val vip = VipContact(
            contactId = contactId,
            name = name,
            phoneNumber = phoneNumber,
            audioTitle = null,
            audioUriString = null
        )
        dao.insertVipContact(vip)
        return OperationResult.Success("VIP contact added.")
    }

    suspend fun assignAudioToVip(vipId: Long, audioTitle: String, audioUriString: String): OperationResult {
        val contacts = dao.getVipContactsSync()
        val target = contacts.find { it.id == vipId } ?: return OperationResult.Error("VIP contact not found.")
        val updated = target.copy(audioTitle = audioTitle, audioUriString = audioUriString)
        dao.updateVipContact(updated)
        return OperationResult.Success("Ringtone assigned to ${target.name}.")
    }

    suspend fun removeAudioFromVip(vipId: Long) {
        val contacts = dao.getVipContactsSync()
        val target = contacts.find { it.id == vipId } ?: return
        val updated = target.copy(audioTitle = null, audioUriString = null)
        dao.updateVipContact(updated)
    }

    suspend fun deleteVipContact(vipContact: VipContact) {
        dao.deleteVipContact(vipContact)
    }

    suspend fun setMasterEnabled(enabled: Boolean) {
        val current = getConfigSync()
        dao.saveConfig(current.copy(masterEnabled = enabled))
    }

    suspend fun setPlaybackMode(mode: String) { // "SEQUENTIAL" or "RANDOM"
        val current = getConfigSync()
        dao.saveConfig(current.copy(playbackMode = mode))
    }

    suspend fun updateLastPlayedIndex(index: Int) {
        val current = getConfigSync()
        dao.saveConfig(current.copy(lastPlayedIndex = index))
    }

    suspend fun logCall(phoneNumber: String, callerName: String?, isVip: Boolean, ringtoneName: String) {
        dao.insertCallLog(
            CallLogItem(
                phoneNumber = phoneNumber,
                callerName = callerName,
                isVip = isVip,
                ringtoneName = ringtoneName,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearCallLogs() {
        dao.clearCallLogs()
    }
}
