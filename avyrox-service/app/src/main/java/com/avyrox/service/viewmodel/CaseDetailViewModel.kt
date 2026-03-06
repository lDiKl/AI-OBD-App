package com.avyrox.service.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avyrox.service.data.cases.DiagnosticCaseRepository
import com.avyrox.service.data.local.DiagnosticCaseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaseDetailViewModel @Inject constructor(
    private val repository: DiagnosticCaseRepository,
) : ViewModel() {

    private val _case = MutableStateFlow<DiagnosticCaseEntity?>(null)
    val case: StateFlow<DiagnosticCaseEntity?> = _case.asStateFlow()

    fun load(localId: String) {
        viewModelScope.launch {
            _case.value = repository.getCase(localId)
        }
    }
}
