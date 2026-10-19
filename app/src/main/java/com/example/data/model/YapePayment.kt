package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus {
    PENDING,
    SUCCESS,
    FAILED
}

@Entity(tableName = "yape_payments")
data class YapePayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val amount: Double,
    val currency: String = "PEN",
    val timestamp: Long = System.currentTimeMillis(),
    val rawTitle: String = "",
    val rawBody: String = "",
    val packageName: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val httpStatusCode: Int? = null,
    val responseBody: String? = null,
    val errorMessage: String? = null,
    val attempts: Int = 0,
    val syncedAt: Long? = null
)
