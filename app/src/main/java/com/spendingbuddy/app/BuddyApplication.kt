package com.spendingbuddy.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.spendingbuddy.app.p2p.PeerSyncManager
import com.spendingbuddy.app.p2p.PeerSyncWorker
import computer.iroh.IrohAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

enum class StartupPhase { LOADING, READY, FAILED }

class BuddyApplication : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startupState0 = MutableStateFlow(StartupPhase.LOADING)
    val startupState: StateFlow<StartupPhase> get() = startupState0
    private var startupJob: Job? = null
    private var manager: PeerSyncManager? = null
    private var foreground = false

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                foreground = true
                manager?.onForeground()
                if (startupState0.value == StartupPhase.READY) startupScope.launch {
                    runCatching { FinishCountdownNotifications.refresh(this@BuddyApplication, BuddyRepository.get(this@BuddyApplication).state.value) }
                }
            }
            override fun onStop(owner: LifecycleOwner) { foreground = false; manager?.onBackground() }
        })
        retryStartup()
    }

    /** Real local/native preparation; no minimum splash delay or dependency on peer reachability. */
    fun retryStartup() {
        if (startupJob?.isActive == true) return
        startupState0.value = StartupPhase.LOADING
        startupJob = startupScope.launch {
            try {
                val repo = BuddyRepository.get(this@BuddyApplication)
                IrohAndroid.installAndroidContext(this@BuddyApplication)
                // Optional system scheduling must never prevent access to the local notebook.
                runCatching { BuddyNotifications.ensureChannels(this@BuddyApplication) }
                runCatching { BuddyNotifications.schedule(this@BuddyApplication, repo.state.value.reminders, repo.state.value.finishCountdownLeadMinutes) }
                runCatching { PeerSyncWorker.schedule(this@BuddyApplication) }
                runCatching { com.spendingbuddy.app.detect.DetectionNotifications.ensureChannel(this@BuddyApplication) }
                val preparedManager = PeerSyncManager.get(this@BuddyApplication)
                withContext(Dispatchers.Main.immediate) {
                    manager = preparedManager
                    startupState0.value = StartupPhase.READY
                    if (foreground) preparedManager.onForeground()
                }
                preparedManager.ensureIdentity()
                runCatching { repo.releaseHolds() }
                runCatching { FinishCountdownNotifications.refresh(this@BuddyApplication, repo.state.value) }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { startupState0.value = StartupPhase.FAILED }
            catch (_: LinkageError) { startupState0.value = StartupPhase.FAILED }
        }
    }

    suspend fun awaitReady(): Boolean = startupState.first { it != StartupPhase.LOADING } == StartupPhase.READY
}
