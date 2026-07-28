package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppConfig
import com.example.data.CallLogItem
import com.example.data.GeneralTrack
import com.example.data.OperationResult
import com.example.data.RingtoneRepository
import com.example.data.VipContact
import com.example.service.RingtonePlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val config: AppConfig = AppConfig(),
    val generalTracks: List<GeneralTrack> = emptyList(),
    val vipContacts: List<VipContact> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val previewingUri: String? = null,
    val userMessage: String? = null,
    val isSimulatingCall: Boolean = false,
    val simulatedNumber: String = ""
)

class RingtoneViewModel(private val repository: RingtoneRepository) : ViewModel() {

    private val _previewingUri = MutableStateFlow<String?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isSimulatingCall = MutableStateFlow(false)
    private val _simulatedNumber = MutableStateFlow("")

    private var previewMediaPlayer: MediaPlayer? = null

    private data class RepoData(
        val config: AppConfig,
        val generalTracks: List<GeneralTrack>,
        val vipContacts: List<VipContact>,
        val callLogs: List<CallLogItem>
    )

    private val repoFlow = combine(
        repository.config,
        repository.generalTracks,
        repository.vipContacts,
        repository.callLogs
    ) { config, general, vip, logs ->
        RepoData(config, general, vip, logs)
    }

    val uiState: StateFlow<UiState> = combine(
        repoFlow,
        _previewingUri,
        _userMessage,
        _isSimulatingCall,
        _simulatedNumber
    ) { repo, previewUri, msg, isSim, simNum ->
        UiState(
            config = repo.config,
            generalTracks = repo.generalTracks,
            vipContacts = repo.vipContacts,
            callLogs = repo.callLogs,
            previewingUri = previewUri,
            userMessage = msg,
            isSimulatingCall = isSim,
            simulatedNumber = simNum
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun toggleMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMasterEnabled(enabled)
            _userMessage.value = if (enabled) "Ringtone Mixer Activated" else "Ringtone Mixer Deactivated"
        }
    }

    fun setPlaybackMode(mode: String) {
        viewModelScope.launch {
            repository.setPlaybackMode(mode)
            _userMessage.value = "Playback order set to: ${if (mode == "SEQUENTIAL") "Sequential" else "Random"}"
        }
    }

    fun addGeneralTrack(title: String, uriString: String, durationMs: Long = 0) {
        viewModelScope.launch {
            when (val result = repository.addGeneralTrack(title, uriString, durationMs)) {
                is OperationResult.Success -> {
                    _userMessage.value = result.message ?: "Song added successfully"
                }
                is OperationResult.Error -> {
                    _userMessage.value = result.reason
                }
            }
        }
    }

    fun deleteGeneralTrack(track: GeneralTrack) {
        viewModelScope.launch {
            repository.deleteGeneralTrack(track)
            _userMessage.value = "Removed track: ${track.title}"
        }
    }

    fun addVipContact(contactId: String, name: String, phoneNumber: String) {
        viewModelScope.launch {
            when (val result = repository.addVipContact(contactId, name, phoneNumber)) {
                is OperationResult.Success -> {
                    _userMessage.value = result.message ?: "VIP contact added"
                }
                is OperationResult.Error -> {
                    _userMessage.value = result.reason
                }
            }
        }
    }

    fun assignAudioToVip(vipId: Long, audioTitle: String, audioUriString: String) {
        viewModelScope.launch {
            when (val result = repository.assignAudioToVip(vipId, audioTitle, audioUriString)) {
                is OperationResult.Success -> {
                    _userMessage.value = result.message ?: "Ringtone assigned"
                }
                is OperationResult.Error -> {
                    _userMessage.value = result.reason
                }
            }
        }
    }

    fun removeAudioFromVip(vipId: Long) {
        viewModelScope.launch {
            repository.removeAudioFromVip(vipId)
            _userMessage.value = "Removed custom ringtone from VIP contact"
        }
    }

    fun deleteVipContact(vipContact: VipContact) {
        viewModelScope.launch {
            repository.deleteVipContact(vipContact)
            _userMessage.value = "Removed VIP contact: ${vipContact.name}"
        }
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            repository.clearCallLogs()
            _userMessage.value = "Call logs cleared"
        }
    }

    fun simulateIncomingCall(context: Context, phoneNumber: String) {
        _isSimulatingCall.value = true
        _simulatedNumber.value = phoneNumber
        _userMessage.value = "Simulating incoming call from $phoneNumber..."
        RingtonePlaybackService.startPlayback(context, phoneNumber)
    }

    fun stopSimulatedCall(context: Context) {
        _isSimulatingCall.value = false
        _simulatedNumber.value = ""
        _userMessage.value = "Simulated call ended"
        RingtonePlaybackService.stopPlayback(context)
    }

    fun togglePreviewAudio(context: Context, uriString: String) {
        if (_previewingUri.value == uriString) {
            stopPreviewAudio()
        } else {
            stopPreviewAudio()
            try {
                previewMediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, Uri.parse(uriString))
                    setOnCompletionListener {
                        stopPreviewAudio()
                    }
                    prepare()
                    start()
                }
                _previewingUri.value = uriString
            } catch (e: Exception) {
                Log.e("ViewModel", "Preview failed for $uriString", e)
                _userMessage.value = "Could not play audio preview"
                stopPreviewAudio()
            }
        }
    }

    fun stopPreviewAudio() {
        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("ViewModel", "Error stopping preview player", e)
        } finally {
            previewMediaPlayer = null
            _previewingUri.value = null
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPreviewAudio()
    }
}

class RingtoneViewModelFactory(private val repository: RingtoneRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RingtoneViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RingtoneViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
