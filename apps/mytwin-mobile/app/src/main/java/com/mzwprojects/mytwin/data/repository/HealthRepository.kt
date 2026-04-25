package com.mzwprojects.mytwin.data.repository

import android.app.Activity
import com.mzwprojects.mytwin.data.datasource.SamsungHealthDataSource
import com.mzwprojects.mytwin.data.datasource.SamsungHealthMetric
import com.mzwprojects.mytwin.data.datasource.SamsungPermissionRequestResult
import com.mzwprojects.mytwin.data.datasource.SamsungPermissionStatus
import com.mzwprojects.mytwin.data.model.WearableSignal

/**
 * Read-only facade over Samsung Health.
 * ViewModels never touch the data sources directly.
 */
class HealthRepository(
    private val samsungDataSource: SamsungHealthDataSource,
) {

    // ─── Availability ────────────────────────────────────────────────────

    /** True once Samsung Health is connected. */
    val isSamsungConnected: Boolean get() = samsungDataSource.isConnected

    /** Connect to Samsung Health. Call before any permission or data operation. */
    suspend fun connectSamsung(): Boolean = samsungDataSource.connect()

    suspend fun permissionStatus(): SamsungPermissionStatus = samsungDataSource.permissionStatus()

    suspend fun areAllPermissionsGranted(): Boolean = samsungDataSource.areAllPermissionsGranted()

    suspend fun requestPermissions(activity: Activity): SamsungPermissionRequestResult =
        samsungDataSource.requestPermissions(activity)

    suspend fun grantedMetrics(): Set<SamsungHealthMetric> = samsungDataSource.grantedMetrics()

    suspend fun signalsByMetric(): Map<SamsungHealthMetric, WearableSignal> =
        samsungDataSource.signalsByMetric()

    // ─── Data ────────────────────────────────────────────────────────────

    suspend fun lastNightSleepHours(): Float? =
        samsungDataSource.lastNightSleepHours()

    suspend fun stepsToday(): Int? =
        samsungDataSource.stepsToday()

    suspend fun latestRestingHeartRate(): Int? =
        samsungDataSource.latestHeartRate()

    suspend fun representativeSleepHours(): Float? =
        samsungDataSource.representativeSleepHours()

    suspend fun representativeDailySteps(): Int? =
        samsungDataSource.representativeDailySteps()

    suspend fun latestStressLevel(): Int? =
        samsungDataSource.latestStressLevel()
}
