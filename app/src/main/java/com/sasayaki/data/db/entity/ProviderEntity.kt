package com.sasayaki.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// API keys are not stored here; they live in SecurePreferencesStore, keyed by id.
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val speechModel: String? = null, // null: not offered for speech to text
    val cleanupModel: String? = null, // null: not offered for cleanup
    val createdAt: Long = System.currentTimeMillis()
)
