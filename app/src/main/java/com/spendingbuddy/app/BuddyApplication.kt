package com.spendingbuddy.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.spendingbuddy.app.p2p.PeerSyncManager
import com.spendingbuddy.app.p2p.PeerSyncWorker
import computer.iroh.IrohAndroid

class BuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        IrohAndroid.installAndroidContext(this)
        BuddyNotifications.ensureChannels(this)
        BuddyNotifications.schedule(this, BuddyRepository.get(this).state.value.reminders)
        PeerSyncWorker.schedule(this)
        val manager = PeerSyncManager.get(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = manager.onForeground()
            override fun onStop(owner: LifecycleOwner) = manager.onBackground()
        })
    }
}
