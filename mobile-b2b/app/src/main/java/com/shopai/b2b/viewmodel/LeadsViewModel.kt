package com.shopai.b2b.viewmodel

import androidx.lifecycle.ViewModel
import com.shopai.b2b.data.leads.B2BLeadsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Exposes B2BLeadsRepository to Compose screens via Hilt ViewModel.
 */
@HiltViewModel
class LeadsViewModel @Inject constructor(
    val repository: B2BLeadsRepository,
) : ViewModel()
