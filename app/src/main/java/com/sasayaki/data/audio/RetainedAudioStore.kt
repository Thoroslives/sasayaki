package com.sasayaki.data.audio

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RetainedAudioStore keeps the recordings that history entries can be retried from. It
 * only ever deletes files inside its own directory, whatever path a database row holds.
 */
@Singleton
class RetainedAudioStore internal constructor(private val directory: File) {
    @Inject constructor(@ApplicationContext context: Context) : this(File(context.filesDir, DIRECTORY))

    /** retain returns the stored path, reusing [audio] when it already lives in the store. */
    suspend fun retain(audio: File): String? = withContext(Dispatchers.IO) {
        if (owns(audio) && audio.exists()) return@withContext audio.absolutePath
        runCatching {
            directory.mkdirs()
            val retained = File.createTempFile("dictation_", ".wav", directory)
            audio.copyTo(retained, overwrite = true)
            retained.absolutePath
        }.onFailure { Log.e(TAG, "Could not retain audio", it) }.getOrNull()
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (owns(file)) file.delete()
    }

    suspend fun deleteAllExcept(keep: Set<String>) = withContext(Dispatchers.IO) {
        val kept = keep.mapNotNullTo(HashSet()) { runCatching { File(it).canonicalPath }.getOrNull() }
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.canonicalPath !in kept) file.delete()
        }
    }

    private fun owns(file: File): Boolean =
        runCatching { file.canonicalFile.parentFile == directory.canonicalFile }.getOrDefault(false)

    private companion object {
        const val TAG = "RetainedAudioStore"
        const val DIRECTORY = "retained_audio"
    }
}
