package com.avyrox.drive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avyrox.drive.data.auth.FirebaseAuthRepository
import com.avyrox.drive.data.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    authRepository: FirebaseAuthRepository,
) : ViewModel() {

    val email: String = authRepository.currentUser?.email ?: ""

    private val _subscriptionStatus = MutableStateFlow("free")
    val subscriptionStatus: StateFlow<String> = _subscriptionStatus.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _subscriptionStatus.value = subscriptionRepository.getSubscriptionStatus()
            } catch (e: Exception) {
                // keep "free" as default if API is unreachable
            } finally {
                _loading.value = false
            }
        }
    }
}
