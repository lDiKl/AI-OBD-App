package com.shopai.b2b.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopai.b2b.data.cases.DiagnosticCaseRepository
import com.shopai.b2b.data.local.DiagnosticCaseEntity
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
