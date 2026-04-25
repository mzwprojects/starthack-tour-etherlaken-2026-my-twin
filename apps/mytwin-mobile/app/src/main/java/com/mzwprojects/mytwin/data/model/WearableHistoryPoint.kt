package com.mzwprojects.mytwin.data.model

import java.time.Instant

data class WearableHistoryPoint<T>(
    val timestamp: Instant,
    val value: T,
)
