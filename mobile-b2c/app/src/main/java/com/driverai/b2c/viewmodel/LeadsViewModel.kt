package com.driverai.b2c.viewmodel

import androidx.lifecycle.ViewModel
import com.driverai.b2c.data.leads.LeadsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Exposes LeadsRepository to Compose screens via Hilt ViewModel.
 */
@HiltViewModel
class LeadsViewModel @Inject constructor(
    val repository: LeadsRepository,
) : ViewModel()
