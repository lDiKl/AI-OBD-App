package com.shopai.b2b.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopai.b2b.data.auth.FirebaseAuthRepository
import com.shopai.b2b.data.shop.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object NeedsShopSetup : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val shopRepository: ShopRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.signInWithEmail(email, password)
            if (result.isFailure) {
                _state.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Sign-in failed")
                return@launch
            }
            checkShopRegistration()
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.createAccountWithEmail(email, password)
            if (result.isFailure) {
                _state.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
                return@launch
            }
            // new account → always needs shop setup
            _state.value = AuthState.NeedsShopSetup
        }
    }

    private suspend fun checkShopRegistration() {
        val registered = shopRepository.isShopRegistered()
        _state.value = if (registered) AuthState.Authenticated else AuthState.NeedsShopSetup
    }

    fun resetError() { _state.value = AuthState.Idle }
}
