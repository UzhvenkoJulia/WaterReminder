package com.example.waterreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.waterreminder.data.WaterReminderPreferences
import com.example.waterreminder.worker.WaterReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WaterReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val preferences = WaterReminderPreferences(context)
    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val UNIQUE_WORK_NAME = "water_reminder"
    }

    // реактив поток із DataStore, перетвор у гарячі StateFlow для Compose
    val reminderCount: StateFlow<Int> = preferences.reminderCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isReminderEnabled: StateFlow<Boolean> = preferences.isEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val savedIntervalMinutes: StateFlow<Long> = preferences.intervalMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15L)

    fun startReminder(intervalMinutes: Long) {
        val safeInterval = if (intervalMinutes < 15L) 15L else intervalMinutes

        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            safeInterval, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

       val immediateRequest = androidx.work.OneTimeWorkRequestBuilder<WaterReminderWorker>().build()
        workManager.enqueue(immediateRequest)

        viewModelScope.launch {
            preferences.saveReminderSettings(isEnabled = true, intervalMinutes = safeInterval)
        }
    }

    fun stopReminder() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        viewModelScope.launch {
            preferences.setReminderEnabled(false)
        }
    }

    fun clearAll() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        viewModelScope.launch {
            preferences.clearAll()
        }
    }
}