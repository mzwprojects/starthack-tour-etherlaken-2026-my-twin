package com.mzwprojects.mytwin.data.datasource

import android.app.Activity
import android.content.Context
import android.util.Log
import com.mzwprojects.mytwin.data.model.WearableSignal
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.android.sdk.health.data.request.Ordering
import java.time.LocalDateTime

/**
 * Samsung Health Data SDK v1.1.0 wrapper.
 *
 * Requires:
 * - Samsung Health 6.30.2+
 * - Android 10+
 * - Java 17+
 */
class SamsungHealthDataSource(context: Context) {

    private val appContext = context.applicationContext

    private val store: HealthDataStore by lazy {
        HealthDataService.getStore(appContext)
    }

    private val metricPermissions: Map<SamsungHealthMetric, Permission> = mapOf(
        SamsungHealthMetric.SLEEP to Permission.of(DataTypes.SLEEP, AccessType.READ),
        SamsungHealthMetric.STEPS to Permission.of(DataTypes.STEPS, AccessType.READ),
        SamsungHealthMetric.HEART_RATE to Permission.of(DataTypes.HEART_RATE, AccessType.READ),
    )

    private val readPermissions: Set<Permission> = metricPermissions.values.toSet()

    val isConnected: Boolean
        get() = runCatching {
            store
            true
        }.onFailure {
            Log.w(TAG, "Samsung Health Data SDK store is not available", it)
        }.getOrDefault(false)

    fun connect(): Boolean = isConnected

    suspend fun grantedMetrics(activity: Activity? = null): Set<SamsungHealthMetric> {
        return try {
            val grantedPermissions = store.getGrantedPermissions(readPermissions)
            metricPermissions
                .filterValues { it in grantedPermissions }
                .keys
        } catch (error: HealthDataException) {
            handleHealthDataException("Permission check failed", error, activity)
            emptySet()
        } catch (error: Throwable) {
            Log.w(TAG, "Permission check failed", error)
            emptySet()
        }
    }

    suspend fun areAllPermissionsGranted(activity: Activity? = null): Boolean {
        return grantedMetrics(activity).containsAll(SamsungHealthMetric.entries)
    }

    suspend fun requestPermissions(activity: Activity): Boolean {
        return try {
            val grantedPermissions = store.getGrantedPermissions(readPermissions)
            val missingPermissions = readPermissions - grantedPermissions

            if (missingPermissions.isEmpty()) {
                return true
            }

            val newlyGrantedPermissions = store.requestPermissions(
                missingPermissions,
                activity,
            )

            newlyGrantedPermissions.containsAll(missingPermissions)
        } catch (error: HealthDataException) {
            handleHealthDataException("Permission request failed", error, activity)
            false
        } catch (error: Throwable) {
            Log.e(TAG, "Permission request failed", error)
            false
        }
    }

    suspend fun stepsToday(): Int? {
        if (!areAllPermissionsGranted()) return null

        return try {
            val now = LocalDateTime.now()
            val startOfDay = now.toLocalDate().atStartOfDay()

            val filter = LocalTimeFilter.of(startOfDay, now)

            val request = DataType.StepsType.TOTAL.requestBuilder
                .setLocalTimeFilterWithGroup(
                    filter,
                    LocalTimeGroup.of(LocalTimeGroupUnit.HOURLY, 1),
                )
                .setOrdering(Ordering.ASC)
                .build()

            val totalSteps = store.aggregateData(request)
                .dataList
                .sumOf { aggregatedData ->
                    aggregatedData.value ?: 0L
                }

            totalSteps
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        } catch (error: HealthDataException) {
            handleHealthDataException("stepsToday failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "stepsToday failed", error)
            null
        }
    }

    suspend fun lastNightSleepHours(): Float? {
        if (!areAllPermissionsGranted()) return null

        return try {
            val now = LocalDateTime.now()
            val start = now.minusHours(36)

            val filter = LocalTimeFilter.of(start, now)

            val request = DataTypes.SLEEP.readDataRequestBuilder
                .setLocalTimeFilter(filter)
                .setOrdering(Ordering.DESC)
                .build()

            store.readData(request)
                .dataList
                .mapNotNull { dataPoint ->
                    dataPoint.getValue(DataType.SleepType.DURATION)
                }
                .maxOfOrNull { duration ->
                    duration.toMinutes().toFloat() / 60f
                }
        } catch (error: HealthDataException) {
            handleHealthDataException("lastNightSleepHours failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "lastNightSleepHours failed", error)
            null
        }
    }

    suspend fun latestHeartRate(): Int? {
        if (!areAllPermissionsGranted()) return null

        return try {
            val now = LocalDateTime.now()
            val start = now.minusDays(7)

            val filter = LocalTimeFilter.of(start, now)

            val request = DataTypes.HEART_RATE.readDataRequestBuilder
                .setLocalTimeFilter(filter)
                .setOrdering(Ordering.DESC)
                .setLimit(1)
                .build()

            store.readData(request)
                .dataList
                .firstOrNull()
                ?.getValue(DataType.HeartRateType.HEART_RATE)
                ?.toInt()
        } catch (error: HealthDataException) {
            handleHealthDataException("latestHeartRate failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "latestHeartRate failed", error)
            null
        }
    }

    suspend fun signalsByMetric(): Map<SamsungHealthMetric, WearableSignal> =
        SamsungHealthMetric.entries.associateWith { signalByMetric(it) }

    suspend fun signalByMetric(metric: SamsungHealthMetric): WearableSignal {
        if (!isConnected) return WearableSignal.UNKNOWN
        if (metric !in grantedMetrics()) return WearableSignal.UNKNOWN

        val now = LocalDateTime.now()
        if (hasData(metric, now.minusDays(3), now)) {
            return WearableSignal.ACTIVE
        }

        return if (hasData(metric, now.minusDays(365), now.minusDays(3))) {
            WearableSignal.DEVICE_PRESENT_NOT_WORN_RECENTLY
        } else {
            WearableSignal.NO_DEVICE_LIKELY
        }
    }

    private suspend fun hasData(
        metric: SamsungHealthMetric,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean {
        return when (metric) {
            SamsungHealthMetric.SLEEP -> hasSleepData(start, end)
            SamsungHealthMetric.STEPS -> hasStepsData(start, end)
            SamsungHealthMetric.HEART_RATE -> hasHeartRateData(start, end)
        }
    }

    private suspend fun hasSleepData(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean {
        return try {
            val request = DataTypes.SLEEP.readDataRequestBuilder
                .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                .setOrdering(Ordering.DESC)
                .setLimit(1)
                .build()

            store.readData(request).dataList.isNotEmpty()
        } catch (error: HealthDataException) {
            handleHealthDataException("hasSleepData failed", error)
            false
        } catch (error: Throwable) {
            Log.w(TAG, "hasSleepData failed", error)
            false
        }
    }

    private suspend fun hasStepsData(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean {
        return try {
            val request = DataType.StepsType.TOTAL.requestBuilder
                .setLocalTimeFilterWithGroup(
                    LocalTimeFilter.of(start, end),
                    LocalTimeGroup.of(LocalTimeGroupUnit.DAILY, 1),
                )
                .setOrdering(Ordering.ASC)
                .build()

            store.aggregateData(request).dataList.any { aggregatedData ->
                (aggregatedData.value ?: 0L) > 0L
            }
        } catch (error: HealthDataException) {
            handleHealthDataException("hasStepsData failed", error)
            false
        } catch (error: Throwable) {
            Log.w(TAG, "hasStepsData failed", error)
            false
        }
    }

    private suspend fun hasHeartRateData(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean {
        return try {
            val request = DataTypes.HEART_RATE.readDataRequestBuilder
                .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                .setOrdering(Ordering.DESC)
                .setLimit(1)
                .build()

            store.readData(request).dataList.isNotEmpty()
        } catch (error: HealthDataException) {
            handleHealthDataException("hasHeartRateData failed", error)
            false
        } catch (error: Throwable) {
            Log.w(TAG, "hasHeartRateData failed", error)
            false
        }
    }

    private fun handleHealthDataException(
        message: String,
        error: HealthDataException,
        activity: Activity? = null,
    ) {
        when (error) {
            is ResolvablePlatformException -> {
                Log.w(TAG, "$message: resolvable Samsung Health platform error", error)

                if (activity != null && error.hasResolution) {
                    error.resolve(activity)
                }
            }

            is AuthorizationException -> {
                Log.w(TAG, "$message: authorization error", error)
            }

            is InvalidRequestException -> {
                Log.w(TAG, "$message: invalid request", error)
            }

            is PlatformInternalException -> {
                Log.w(TAG, "$message: Samsung Health platform internal error", error)
            }

            else -> {
                Log.w(TAG, message, error)
            }
        }
    }

    companion object {
        private const val TAG = "SamsungHealth"
    }
}

enum class SamsungHealthMetric {
    SLEEP,
    STEPS,
    HEART_RATE,
}
