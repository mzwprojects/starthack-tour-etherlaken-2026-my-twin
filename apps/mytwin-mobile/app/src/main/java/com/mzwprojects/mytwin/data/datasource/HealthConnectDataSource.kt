package com.mzwprojects.mytwin.data.datasource

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.mzwprojects.mytwin.data.model.WearableSignal
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Thin wrapper around [HealthConnectClient]. All public reads:
 *  - return defaulted values rather than throwing,
 *  - silently no-op when Health Connect is unavailable.
 *
 * We never *write* to Health Connect.
 */
class HealthConnectDataSource(private val context: Context) {

    /** Cached after first successful resolve; null if unavailable. */
    private val client: HealthConnectClient? by lazy {
        val status = runCatching { HealthConnectClient.getSdkStatus(context) }
            .onFailure { Log.e(TAG, "getSdkStatus threw", it) }
            .getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)
        Log.d(TAG, "SDK status=$status  (AVAILABLE=${HealthConnectClient.SDK_AVAILABLE}, " +
            "UPDATE_REQUIRED=${HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED})")
        when (status) {
            HealthConnectClient.SDK_AVAILABLE -> {
                runCatching { HealthConnectClient.getOrCreate(context) }
                    .onSuccess { Log.d(TAG, "HealthConnectClient created successfully") }
                    .onFailure { Log.e(TAG, "getOrCreate threw", it) }
                    .getOrNull()
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Log.w(TAG, "Health Connect provider needs an update")
                null
            }
            else -> {
                Log.w(TAG, "Health Connect not available (status=$status)")
                null
            }
        }
    }

    val isAvailable: Boolean get() = client != null

    /** True when HC is installed but the provider app needs to be updated. */
    val needsProviderUpdate: Boolean get() =
        runCatching { HealthConnectClient.getSdkStatus(context) }
            .getOrDefault(HealthConnectClient.SDK_UNAVAILABLE) ==
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    // ─── Permission contract ─────────────────────────────────────────────

    suspend fun grantedPermissions(): Set<String> =
        runCatching { client?.permissionController?.getGrantedPermissions().orEmpty() }
            .onSuccess { Log.d(TAG, "Granted permissions (${it.size}): $it") }
            .onFailure { Log.e(TAG, "getGrantedPermissions threw", it) }
            .getOrElse { emptySet() }

    // ─── Wearable-presence detection ─────────────────────────────────────

    suspend fun classifySignal(metric: HealthMetric): WearableSignal {
        val client = client ?: return WearableSignal.UNKNOWN
        val granted = grantedPermissions()
        if (metric.permission !in granted) return WearableSignal.UNKNOWN

        val now = Instant.now()
        val recent = readSamplesCount(client, metric.recordType, now.minus(RECENT_WINDOW), now)
        if (recent > 0) return WearableSignal.ACTIVE

        val historical = readSamplesCount(
            client = client,
            recordType = metric.recordType,
            from = now.minus(HISTORICAL_WINDOW),
            to = now.minus(RECENT_WINDOW),
        )
        return if (historical > 0) {
            WearableSignal.DEVICE_PRESENT_NOT_WORN_RECENTLY
        } else {
            WearableSignal.NO_DEVICE_LIKELY
        }
    }

    // ─── Aggregations used by HealthRepository ───────────────────────────

    suspend fun lastNightSleepHours(): Float? = withClient { client ->
        val now = Instant.now()
        val from = now.minus(Duration.ofHours(36))
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, now),
            ),
        )
        response.records
            .maxByOrNull { it.endTime }
            ?.let { Duration.between(it.startTime, it.endTime).toMinutes() / 60f }
    }

    suspend fun stepsToday(): Int? = withClient { client ->
        val now = Instant.now()
        val startOfDay = now.minus(Duration.ofHours(24)) // good enough for a hackathon
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
            ),
        )
        response.records.sumOf { it.count.toInt() }.takeIf { it > 0 || response.records.isNotEmpty() }
    }

    suspend fun latestRestingHeartRate(): Int? = withClient { client ->
        val now = Instant.now()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofDays(7)), now),
            ),
        )
        response.records.maxByOrNull { it.time }?.beatsPerMinute?.toInt()
    }

    // ─── Internals ───────────────────────────────────────────────────────

    private suspend fun <T : Record> readSamplesCount(
        client: HealthConnectClient,
        recordType: KClass<T>,
        from: Instant,
        to: Instant,
    ): Int = runCatching {
        client.readRecords(
            ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = TimeRangeFilter.between(from, to),
                pageSize = 1,
            ),
        ).records.size
    }.onFailure { Log.w(TAG, "readSamplesCount failed for ${recordType.simpleName}", it) }
        .getOrDefault(0)

    private suspend fun <T> withClient(block: suspend (HealthConnectClient) -> T?): T? {
        val client = client ?: return null
        return runCatching { block(client) }
            .onFailure { Log.w(TAG, "Health Connect read failed", it) }
            .getOrNull()
    }

    private companion object {
        const val TAG = "HealthConnect"
        val RECENT_WINDOW: Duration = Duration.ofDays(3)
        val HISTORICAL_WINDOW: Duration = Duration.ofDays(365)
    }
}

/**
 * Single source of truth for the metrics we care about: the HC record class +
 * its read-permission string. Add new metrics here and they're automatically
 * surfaced in the permission set.
 */
enum class HealthMetric(
    val recordType: KClass<out Record>,
    val permission: String,
) {
    SLEEP(SleepSessionRecord::class, HealthPermission.getReadPermission(SleepSessionRecord::class)),
    STEPS(StepsRecord::class, HealthPermission.getReadPermission(StepsRecord::class)),
    HEART_RATE(HeartRateRecord::class, HealthPermission.getReadPermission(HeartRateRecord::class)),
    RESTING_HEART_RATE(
        RestingHeartRateRecord::class,
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    ),
    HEART_RATE_VARIABILITY(
        HeartRateVariabilityRmssdRecord::class,
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
    ),
    OXYGEN_SATURATION(
        OxygenSaturationRecord::class,
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    ),
    TOTAL_CALORIES(
        TotalCaloriesBurnedRecord::class,
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    );

    companion object {
        val ALL_PERMISSIONS: Set<String> = entries.map { it.permission }.toSet()
    }
}