package com.avyrox.service.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avyrox.service.data.shop.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SetupState {
    object Idle : SetupState()
    object Loading : SetupState()
    object Done : SetupState()
    data class Error(val message: String) : SetupState()
}

@HiltViewModel
class ShopSetupViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SetupState>(SetupState.Idle)
    val state: StateFlow<SetupState> = _state.asStateFlow()

    fun setupShop(shopName: String, address: String, phone: String, email: String) {
        if (shopName.isBlank()) {
            _state.value = SetupState.Error("Shop name is required")
            return
        }
        viewModelScope.launch {
            _state.value = SetupState.Loading
            try {
                shopRepository.setupShop(shopName, address, phone, email)
                _state.value = SetupState.Done
            } catch (e: Exception) {
                _state.value = SetupState.Error(e.message ?: "Setup failed")
            }
        }
    }

    fun resetError() { _state.value = SetupState.Idle }
}
