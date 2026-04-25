package com.mzwprojects.mytwin.data.repository

import android.app.Activity
import com.mzwprojects.mytwin.data.datasource.HealthConnectDataSource
import com.mzwprojects.mytwin.data.datasource.HealthMetric
import com.mzwprojects.mytwin.data.datasource.SamsungHealthDataSource
import com.mzwprojects.mytwin.data.model.WearableSignal

/**
 * Read-only facade over Samsung Health (primary) + Health Connect (fallback).
 * ViewModels never touch the data sources directly.
 */
class HealthRepository(
    private val samsungDataSource: SamsungHealthDataSource,
    private val hcDataSource: HealthConnectDataSource,
) {

    // ─── Availability ────────────────────────────────────────────────────

    /** True once Samsung Health is connected. */
    val isSamsungConnected: Boolean get() = samsungDataSource.isConnected

    /** Connect to Samsung Health. Call before any permission or data operation. */
    suspend fun connectSamsung(): Boolean = samsungDataSource.connect()

    suspend fun areAllPermissionsGranted(): Boolean = samsungDataSource.areAllPermissionsGranted()

    suspend fun requestPermissions(activity: Activity) = samsungDataSource.requestPermissions(activity)

    // ─── Health Connect fallback (kept for non-Samsung devices) ──────────

    val isHcAvailable: Boolean get() = hcDataSource.isAvailable
    val isHcNeedsUpdate: Boolean get() = hcDataSource.needsProviderUpdate

    suspend fun grantedHcPermissions(): Set<String> = hcDataSource.grantedPermissions()

    suspend fun signalsByMetric(): Map<HealthMetric, WearableSignal> =
        HealthMetric.entries.associateWith { hcDataSource.classifySignal(it) }

    // ─── Data — Samsung Health first, HC fallback ────────────────────────

    suspend fun lastNightSleepHours(): Float? =
        samsungDataSource.lastNightSleepHours() ?: hcDataSource.lastNightSleepHours()

    suspend fun stepsToday(): Int? =
        samsungDataSource.stepsToday() ?: hcDataSource.stepsToday()

    suspend fun latestRestingHeartRate(): Int? =
        samsungDataSource.latestHeartRate() ?: hcDataSource.latestRestingHeartRate()
}
