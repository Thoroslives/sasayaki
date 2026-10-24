package com.sasayaki.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sasayaki.data.db.dao.DictationDao
import com.sasayaki.data.db.dao.LifetimeStatsDao
import com.sasayaki.data.db.dao.PostProcessingPromptDao
import com.sasayaki.data.db.dao.ProfileDao
import com.sasayaki.data.db.dao.ProviderDao
import com.sasayaki.data.db.dao.TextReplacementRuleDao
import com.sasayaki.data.db.entity.Dictation
import com.sasayaki.data.db.entity.LifetimeStatsEntity
import com.sasayaki.data.db.entity.PostProcessingPromptEntity
import com.sasayaki.data.db.entity.ProfileEntity
import com.sasayaki.data.db.entity.ProviderEntity
import com.sasayaki.data.db.entity.TextReplacementRuleEntity

@Database(
    entities = [
        Dictation::class,
        ProfileEntity::class,
        TextReplacementRuleEntity::class,
        PostProcessingPromptEntity::class,
        LifetimeStatsEntity::class,
        ProviderEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class SasayakiDatabase : RoomDatabase() {
    abstract fun dictationDao(): DictationDao
    abstract fun profileDao(): ProfileDao
    abstract fun textReplacementRuleDao(): TextReplacementRuleDao
    abstract fun postProcessingPromptDao(): PostProcessingPromptDao
    abstract fun lifetimeStatsDao(): LifetimeStatsDao
    abstract fun providerDao(): ProviderDao
}
