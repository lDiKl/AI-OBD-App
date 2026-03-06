package com.avyrox.drive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avyrox.drive.data.scan.ScanRepository
import com.avyrox.drive.data.scan.ScanSessionWithOccurrences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScanRepository,
) : ViewModel() {

    val sessions: StateFlow<List<ScanSessionWithOccurrences>> = repository.sessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun deleteSession(sessionId: String) {
        viewModelScope.launch { repository.deleteSession(sessionId) }
    }
}
