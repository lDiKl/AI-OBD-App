package com.avyrox.service.viewmodel

import androidx.lifecycle.ViewModel
import com.avyrox.service.data.leads.B2BLeadsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Exposes B2BLeadsRepository to Compose screens via Hilt ViewModel.
 */
@HiltViewModel
class LeadsViewModel @Inject constructor(
    val repository: B2BLeadsRepository,
) : ViewModel()
