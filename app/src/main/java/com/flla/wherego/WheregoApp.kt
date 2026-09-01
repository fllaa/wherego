package com.flla.wherego

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.flla.wherego.core.datastore.AppLockController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WheregoApp : Application() {
    @Inject
    lateinit var lockController: AppLockController

    /**
     * `ProcessLifecycleOwner`, not `Activity.onStop`: it debounces activity transitions and
     * configuration changes, so a rotation or `AppLocale`'s locale-driven `recreate()` never counts
     * as leaving the app. Only a real trip to the background does — and even then the controller
     * holds a grace window before it re-arms the gate.
     */
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = lockController.onForeground()

                override fun onStop(owner: LifecycleOwner) = lockController.onBackground()
            },
        )
    }
}
