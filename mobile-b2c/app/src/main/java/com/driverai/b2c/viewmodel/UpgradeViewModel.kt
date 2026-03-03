package com.driverai.b2c.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverai.b2c.data.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class CheckoutReady(val url: String) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun startCheckout() {
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                val url = repository.getCheckoutUrl()
                _state.value = State.CheckoutReady(url)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Failed to start checkout")
            }
        }
    }

    fun resetState() {
        _state.value = State.Idle
    }
}
