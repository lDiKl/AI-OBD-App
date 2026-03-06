package com.avyrox.service.util

import android.util.Log
import com.avyrox.service.data.cases.DiagnosticCaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncManager"

/**
 * Watches network connectivity and syncs pending cases whenever we go online.
 */
@Singleton
class SyncManager @Inject constructor(
    private val connectivityObserver: ConnectivityObserver,
    private val caseRepository: DiagnosticCaseRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        connectivityObserver.isOnline
            .onEach { isOnline ->
                if (isOnline) {
                    Log.d(TAG, "Network available — syncing pending cases")
                    scope.launch { caseRepository.syncPending() }
                }
            }
            .launchIn(scope)
    }
}
