package com.sasayaki.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BubbleServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val running: StateFlow<Boolean> = BubbleService.runningState

    fun start() = BubbleService.start(context)

    fun stop() = BubbleService.stop(context)
}
