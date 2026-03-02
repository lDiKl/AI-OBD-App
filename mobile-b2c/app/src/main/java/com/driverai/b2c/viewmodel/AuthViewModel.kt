package com.driverai.b2c.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverai.b2c.data.auth.FirebaseAuthRepository
import com.driverai.b2c.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val apiService: ApiService,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.signInWithEmail(email, password)
            if (result.isSuccess) {
                verifyWithBackend()
            } else {
                _state.value = AuthState.Error(result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed")
            }
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Please fill in all fields")
            return
        }
        if (password.length < 6) {
            _state.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.createAccountWithEmail(email, password)
            if (result.isSuccess) {
                verifyWithBackend()
            } else {
                _state.value = AuthState.Error(result.exceptionOrNull()?.localizedMessage ?: "Registration failed")
            }
        }
    }

    /** After Firebase sign-in, register/sync the user in the backend DB. */
    private suspend fun verifyWithBackend() {
        try {
            apiService.verifyAuth()
            _state.value = AuthState.Success
        } catch (e: Exception) {
            // Backend call failed but Firebase auth succeeded — still let user in.
            // Backend will create the user on next request.
            _state.value = AuthState.Success
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}
