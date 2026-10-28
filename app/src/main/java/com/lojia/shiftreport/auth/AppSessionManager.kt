package com.lojia.shiftreport.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.lojia.shiftreport.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * POS-style application session manager.
 * Implements [DefaultLifecycleObserver] with [ProcessLifecycleOwner] and listens
 * for screen-off events to enforce instant lock on screen-off or app minimization.
 */
object AppSessionManager : DefaultLifecycleObserver {

    private const val TAG = "AppSessionManager"

    private val _shouldLockApp = MutableStateFlow(false)
    val shouldLockApp: StateFlow<Boolean> = _shouldLockApp.asStateFlow()

    private var isInitialized = false
    private var isReceiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                AppLogger.d(TAG, "Screen display turned OFF -> Triggering instant POS session lock")
                triggerLock()
            }
        }
    }

    /**
     * Initializes session tracking with ProcessLifecycleOwner and screen-off broadcast listener.
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        // Register process lifecycle observer on main thread
        Handler(Looper.getMainLooper()).post {
            try {
                ProcessLifecycleOwner.get().lifecycle.addObserver(this)
                AppLogger.d(TAG, "ProcessLifecycleOwner observer registered successfully")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to register ProcessLifecycleOwner observer", e)
            }
        }

        // Register Intent.ACTION_SCREEN_OFF broadcast receiver
        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            ContextCompat.registerReceiver(
                context.applicationContext,
                screenReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
            AppLogger.d(TAG, "Screen-off broadcast receiver registered successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to register screen-off broadcast receiver", e)
        }
    }

    private var isAppInForeground = false

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
    }

    /**
     * Called when the app process moves to the background (user pressed Home, switched apps).
     */
    override fun onStop(owner: LifecycleOwner) {
        if (isAppInForeground) {
            isAppInForeground = false
            AppLogger.d(TAG, "App moved to background (Process onStop) -> Triggering instant POS session lock")
            triggerLock()
        }
    }

    /**
     * Manually triggers an app lock.
     */
    fun triggerLock() {
        _shouldLockApp.value = true
    }

    /**
     * Acknowledges that the app has locked the session, resetting the trigger flag.
     */
    fun onAppLocked() {
        _shouldLockApp.value = false
    }

    /**
     * Unregisters broadcast receiver and lifecycle observer.
     */
    fun cleanup(context: Context) {
        if (isReceiverRegistered) {
            try {
                context.applicationContext.unregisterReceiver(screenReceiver)
                isReceiverRegistered = false
                AppLogger.d(TAG, "Screen-off broadcast receiver unregistered")
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error unregistering screen receiver: ${e.message}")
            }
        }

        Handler(Looper.getMainLooper()).post {
            try {
                ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error removing ProcessLifecycleOwner observer: ${e.message}")
            }
        }

        isInitialized = false
    }
}
