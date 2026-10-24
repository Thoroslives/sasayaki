package com.sasayaki.data.db

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sasayaki.di.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SasayakiDatabase::class.java)

    @Test
    fun `providers migration keeps profiles and matches the schema`() {
        helper.createDatabase(DB, 7).use { db ->
            db.execSQL(
                """
                INSERT INTO profiles (id, name, isActive, asrModel, language, llmEnabled, llmModel, profilePrompt,
                    outputStyle, rewriteMode, summarizeMode, emojiAllowed, selectedRuleIds, selectedPromptIds, createdAt, updatedAt)
                VALUES (1, 'Chat', 1, 'large-v3', 'en', 1, 'qwen', '', 'STANDARD', 'FIX', 'NONE', 0, '', '', 0, 0)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(DB, 8, true, *DatabaseModule.migrations).use { db ->
            db.query("SELECT name, asrModel, speechProviderId, cleanupProviderId FROM profiles").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Chat", cursor.getString(0))
                assertEquals("large-v3", cursor.getString(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals(true, cursor.isNull(3))
            }
            db.query("SELECT COUNT(*) FROM providers").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
