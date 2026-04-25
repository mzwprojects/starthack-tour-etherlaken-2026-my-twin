package com.mzwprojects.mytwin.data.datasource

import android.app.Activity
import android.content.Context
import android.util.Log
import com.mzwprojects.mytwin.data.model.WearableSignal
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.ErrorCode
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.device.DeviceGroup
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.request.ReadSourceFilter
import java.time.LocalDateTime
import kotlin.math.roundToInt

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
        SamsungHealthMetric.STRESS to Permission.of(DataTypes.ENERGY_SCORE, AccessType.READ),
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

    suspend fun permissionStatus(activity: Activity? = null): SamsungPermissionStatus {
        return try {
            val grantedPermissions = store.getGrantedPermissions(readPermissions)
            SamsungPermissionStatus.Success(
                metricPermissions
                    .filterValues { it in grantedPermissions }
                    .keys,
            )
        } catch (error: AuthorizationException) {
            handleHealthDataException("Permission check failed", error, activity)
            if (error.errorCode == ErrorCode.ERR_ACCESS_CONTROL) {
                SamsungPermissionStatus.PolicyDenied
            } else {
                SamsungPermissionStatus.Error(error.errorMessage)
            }
        } catch (error: HealthDataException) {
            handleHealthDataException("Permission check failed", error, activity)
            SamsungPermissionStatus.Error(error.errorMessage)
        } catch (error: Throwable) {
            Log.w(TAG, "Permission check failed", error)
            SamsungPermissionStatus.Error(error.message)
        }
    }

    suspend fun grantedMetrics(activity: Activity? = null): Set<SamsungHealthMetric> =
        when (val status = permissionStatus(activity)) {
            is SamsungPermissionStatus.Success -> status.grantedMetrics
            SamsungPermissionStatus.PolicyDenied -> emptySet()
            is SamsungPermissionStatus.Error -> emptySet()
        }

    suspend fun areAllPermissionsGranted(activity: Activity? = null): Boolean {
        return grantedMetrics(activity).containsAll(SamsungHealthMetric.entries)
    }

    suspend fun requestPermissions(activity: Activity): SamsungPermissionRequestResult {
        return try {
            val grantedPermissions = store.getGrantedPermissions(readPermissions)
            val missingPermissions = readPermissions - grantedPermissions

            if (missingPermissions.isEmpty()) {
                return SamsungPermissionRequestResult.Granted
            }

            val newlyGrantedPermissions = store.requestPermissions(
                missingPermissions,
                activity,
            )

            if (newlyGrantedPermissions.containsAll(missingPermissions)) {
                SamsungPermissionRequestResult.Granted
            } else {
                SamsungPermissionRequestResult.Denied
            }
        } catch (error: AuthorizationException) {
            handleHealthDataException("Permission request failed", error, activity)
            if (error.errorCode == ErrorCode.ERR_ACCESS_CONTROL) {
                SamsungPermissionRequestResult.PolicyDenied
            } else {
                SamsungPermissionRequestResult.Failed(error.errorMessage)
            }
        } catch (error: HealthDataException) {
            handleHealthDataException("Permission request failed", error, activity)
            SamsungPermissionRequestResult.Failed(error.errorMessage)
        } catch (error: Throwable) {
            Log.e(TAG, "Permission request failed", error)
            SamsungPermissionRequestResult.Failed(error.message)
        }
    }

    suspend fun stepsToday(): Int? {
        if (SamsungHealthMetric.STEPS !in grantedMetrics()) return null

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
        if (SamsungHealthMetric.SLEEP !in grantedMetrics()) return null

        return try {
            val now = LocalDateTime.now()
            val start = now.minusHours(36)
            readSleepDurations(start, now)
                .maxOrNull()
        } catch (error: HealthDataException) {
            handleHealthDataException("lastNightSleepHours failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "lastNightSleepHours failed", error)
            null
        }
    }

    suspend fun latestHeartRate(): Int? {
        if (SamsungHealthMetric.HEART_RATE !in grantedMetrics()) return null

        return try {
            val now = LocalDateTime.now()
            val start = now.minusDays(7)
            readHeartRateValues(start, now)
                .firstOrNull()
                ?.roundToInt()
        } catch (error: HealthDataException) {
            handleHealthDataException("latestHeartRate failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "latestHeartRate failed", error)
            null
        }
    }

    suspend fun representativeSleepHours(days: Long = 14): Float? {
        if (SamsungHealthMetric.SLEEP !in grantedMetrics()) return null

        return try {
            val now = LocalDateTime.now()
            val durations = readSleepDurations(now.minusDays(days), now)

            durations
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toFloat()
        } catch (error: HealthDataException) {
            handleHealthDataException("representativeSleepHours failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "representativeSleepHours failed", error)
            null
        }
    }

    suspend fun representativeDailySteps(days: Long = 14): Int? {
        if (SamsungHealthMetric.STEPS !in grantedMetrics()) return null

        return try {
            val now = LocalDateTime.now()
            val request = DataType.StepsType.TOTAL.requestBuilder
                .setLocalTimeFilterWithGroup(
                    LocalTimeFilter.of(now.minusDays(days), now),
                    LocalTimeGroup.of(LocalTimeGroupUnit.DAILY, 1),
                )
                .setOrdering(Ordering.ASC)
                .build()

            val dailyTotals = store.aggregateData(request)
                .dataList
                .mapNotNull { aggregatedData ->
                    aggregatedData.value?.takeIf { it > 0L }
                }

            dailyTotals
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToInt()
        } catch (error: HealthDataException) {
            handleHealthDataException("representativeDailySteps failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "representativeDailySteps failed", error)
            null
        }
    }

    suspend fun latestStressLevel(days: Long = 7): Int? {
        if (SamsungHealthMetric.STRESS !in grantedMetrics()) return null

        return try {
            val today = LocalDateTime.now().toLocalDate()
            val request = DataTypes.ENERGY_SCORE.readDataRequestBuilder
                .setLocalDateFilter(
                    com.samsung.android.sdk.health.data.request.LocalDateFilter.of(
                        today.minusDays(days),
                        today,
                    ),
                )
                .setOrdering(Ordering.DESC)
                .setLimit(1)
                .build()

            store.readData(request)
                .dataList
                .firstOrNull()
                ?.getValue(DataType.EnergyScoreType.ENERGY_SCORE)
                ?.let(::mapEnergyScoreToStressLevel)
        } catch (error: HealthDataException) {
            handleHealthDataException("latestStressLevel failed", error)
            null
        } catch (error: Throwable) {
            Log.w(TAG, "latestStressLevel failed", error)
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
            SamsungHealthMetric.STRESS -> hasStressData(start, end)
        }
    }

    private suspend fun hasSleepData(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean {
        return try {
            readSleepDurations(start, end).isNotEmpty()
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
            readHeartRateValues(start, end).isNotEmpty()
        } catch (error: HealthDataException) {
            handleHealthDataException("hasHeartRateData failed", error)
            false
        } catch (error: Throwable) {
            Log.w(TAG, "hasHeartRateData failed", error)
            false
        }
    }

    private suspend fun hasStressData(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean {
        return try {
            val request = DataTypes.ENERGY_SCORE.readDataRequestBuilder
                .setLocalDateFilter(
                    com.samsung.android.sdk.health.data.request.LocalDateFilter.of(
                        start.toLocalDate(),
                        end.toLocalDate(),
                    ),
                )
                .setOrdering(Ordering.DESC)
                .setLimit(1)
                .build()

            store.readData(request).dataList.isNotEmpty()
        } catch (error: HealthDataException) {
            handleHealthDataException("hasStressData failed", error)
            false
        } catch (error: Throwable) {
            Log.w(TAG, "hasStressData failed", error)
            false
        }
    }

    private fun mapEnergyScoreToStressLevel(energyScore: Float): Int {
        val clampedScore = energyScore.coerceIn(0f, 100f)
        return (10f - (clampedScore / 100f) * 9f).roundToInt().coerceIn(1, 10)
    }

    private suspend fun readSleepDurations(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Float> {
        val filter = LocalTimeFilter.of(start, end)
        return readWithSourceFallbacks(
            query = { sourceFilter ->
                val builder = DataTypes.SLEEP.readDataRequestBuilder
                    .setLocalTimeFilter(filter)
                    .setOrdering(Ordering.DESC)
                if (sourceFilter != null) builder.setSourceFilter(sourceFilter)
                store.readData(builder.build()).dataList
                    .flatMap { dataPoint ->
                        buildList {
                            dataPoint.getValue(DataType.SleepType.DURATION)?.let { duration ->
                                add(duration.toMinutes().toFloat() / 60f)
                            }
                            dataPoint.getValue(DataType.SleepType.SESSIONS)
                                ?.map { session -> session.duration.toMinutes().toFloat() / 60f }
                                ?.let(::addAll)
                        }
                    }
            },
        )
    }

    private suspend fun readHeartRateValues(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Float> {
        val filter = LocalTimeFilter.of(start, end)
        return readWithSourceFallbacks(
            query = { sourceFilter ->
                val builder = DataTypes.HEART_RATE.readDataRequestBuilder
                    .setLocalTimeFilter(filter)
                    .setOrdering(Ordering.DESC)
                if (sourceFilter != null) builder.setSourceFilter(sourceFilter)
                store.readData(builder.build()).dataList
                    .flatMap { dataPoint ->
                        buildList {
                            dataPoint.getValue(DataType.HeartRateType.HEART_RATE)?.let(::add)
                            dataPoint.getValue(DataType.HeartRateType.SERIES_DATA)
                                ?.map { seriesPoint -> seriesPoint.heartRate }
                                ?.let(::addAll)
                            dataPoint.getValue(DataType.HeartRateType.MAX_HEART_RATE)?.let(::add)
                            dataPoint.getValue(DataType.HeartRateType.MIN_HEART_RATE)?.let(::add)
                        }
                    }
            },
        )
    }

    private suspend fun <T> readWithSourceFallbacks(
        query: suspend (ReadSourceFilter?) -> List<T>,
    ): List<T> {
        for (sourceFilter in preferredSourceFilters) {
            val result = query(sourceFilter)
            if (result.isNotEmpty()) {
                return result
            }
        }
        return emptyList()
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
        private val preferredSourceFilters: List<ReadSourceFilter?> = listOf(
            null,
            ReadSourceFilter.fromDeviceType(DeviceGroup.WATCH),
            ReadSourceFilter.fromDeviceType(DeviceGroup.RING),
            ReadSourceFilter.fromDeviceType(DeviceGroup.BAND),
            ReadSourceFilter.fromLocalDevice(),
        )
    }
}

enum class SamsungHealthMetric {
    SLEEP,
    STEPS,
    HEART_RATE,
    STRESS,
}

sealed interface SamsungPermissionStatus {
    data class Success(val grantedMetrics: Set<SamsungHealthMetric>) : SamsungPermissionStatus
    data object PolicyDenied : SamsungPermissionStatus
    data class Error(val message: String?) : SamsungPermissionStatus
}

sealed interface SamsungPermissionRequestResult {
    data object Granted : SamsungPermissionRequestResult
    data object Denied : SamsungPermissionRequestResult
    data object PolicyDenied : SamsungPermissionRequestResult
    data class Failed(val message: String?) : SamsungPermissionRequestResult
}
