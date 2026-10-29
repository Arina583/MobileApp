package com.example.mobileapp.models

import java.util.Date

data class SyncLong(
    val id: Long? = null,
    val deviceToken: String,
    val lastSyncAt: Date,
    val lastSequenceNumber: Int,
    val downloadedChangesCount: Int,
)