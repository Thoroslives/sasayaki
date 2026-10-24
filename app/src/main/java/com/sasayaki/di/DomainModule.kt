package com.sasayaki.di

import android.content.Context
import com.sasayaki.domain.transcription.NetworkMonitor
import com.sasayaki.domain.transcription.TranscriptionEngine
import com.sasayaki.domain.transcription.WhisperEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds
    abstract fun bindTranscriptionEngine(engine: WhisperEngine): TranscriptionEngine

    companion object {
        @Provides
        @Singleton
        fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor = NetworkMonitor(context)
    }
}
