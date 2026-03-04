package com.shopai.b2b.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopai.b2b.data.auth.FirebaseAuthRepository
import com.shopai.b2b.data.shop.ShopRepository
import com.shopai.b2b.data.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val subscriptionRepository: SubscriptionRepository,
    authRepository: FirebaseAuthRepository,
) : ViewModel() {

    val email: String = authRepository.currentUser?.email ?: ""

    private val _shopName = MutableStateFlow("")
    val shopName: StateFlow<String> = _shopName.asStateFlow()

    private val _tier = MutableStateFlow("basic")
    val tier: StateFlow<String> = _tier.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _checkoutUrl = MutableStateFlow<String?>(null)
    val checkoutUrl: StateFlow<String?> = _checkoutUrl.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val profile = shopRepository.fetchAndCacheProfile()
                _shopName.value = profile?.name ?: ""
                val status = subscriptionRepository.getSubscriptionTier()
                _tier.value = status
            } catch (_: Exception) {
                val cached = shopRepository.observeProfile()
                // tier stays as whatever was cached
            } finally {
                _loading.value = false
            }
        }
    }

    fun startCheckout(tier: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val url = subscriptionRepository.getCheckoutUrl(tier)
                _checkoutUrl.value = url
            } catch (e: Exception) {
                // ignore — UI handles null
            } finally {
                _loading.value = false
            }
        }
    }

    fun onCheckoutConsumed() { _checkoutUrl.value = null }
}
