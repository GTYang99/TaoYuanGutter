package com.example.taoyuangutter.common

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoAlbumStoreInstrumentedTest {

    @Test
    fun insertFailureReturnsNullWithoutThrowing() = runBlocking {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = ContentResolver.wrap(ThrowingInsertProvider())
        val context = ResolverContext(baseContext, resolver)
        val sourceFile = File(context.cacheDir, "photo-album-store-insert-failure.jpg").apply {
            writeText("test")
        }

        try {
            assertNull(PhotoAlbumStore.copyToSystemAlbum(context, sourceFile))
        } finally {
            sourceFile.delete()
        }
    }

    private class ResolverContext(
        base: Context,
        private val resolver: ContentResolver
    ) : ContextWrapper(base) {
        override fun getContentResolver(): ContentResolver = resolver
    }

    private class ThrowingInsertProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            throw IllegalStateException("Injected MediaStore.insert failure")
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int = 0
    }
}
