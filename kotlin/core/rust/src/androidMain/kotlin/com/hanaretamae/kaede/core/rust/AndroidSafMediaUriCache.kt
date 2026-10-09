package com.hanaretamae.kaede.core.rust

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class AndroidSafMediaUriCache(context: Context) {
    private val database = Database(context.applicationContext)

    @Synchronized
    fun get(treeUri: String, relativePath: String): String? =
        database.readableDatabase.query(
            TABLE,
            arrayOf(COLUMN_DOCUMENT_ID),
            "$COLUMN_TREE_URI = ? AND $COLUMN_RELATIVE_PATH = ?",
            arrayOf(treeUri, relativePath),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    @Synchronized
    fun put(treeUri: String, relativePath: String, documentId: String) {
        val rowId = database.writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            ContentValues().apply {
                put(COLUMN_TREE_URI, treeUri)
                put(COLUMN_RELATIVE_PATH, relativePath)
                put(COLUMN_DOCUMENT_ID, documentId)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        if (rowId == -1L) {
            throw android.database.sqlite.SQLiteException("Private media URI cache update failed.")
        }
    }

    @Synchronized
    fun replace(treeUri: String, documentIdsByPath: Map<String, String>) {
        val writable = database.writableDatabase
        writable.beginTransaction()
        try {
            writable.delete(TABLE, "$COLUMN_TREE_URI = ?", arrayOf(treeUri))
            writable.delete(
                THUMBNAIL_TABLE,
                "$COLUMN_TREE_URI = ?",
                arrayOf(treeUri),
            )
            documentIdsByPath.forEach { (path, documentId) ->
                writable.insertOrThrow(
                    TABLE,
                    null,
                    ContentValues().apply {
                        put(COLUMN_TREE_URI, treeUri)
                        put(COLUMN_RELATIVE_PATH, path)
                        put(COLUMN_DOCUMENT_ID, documentId)
                    },
                )
            }
            writable.setTransactionSuccessful()
        } finally {
            writable.endTransaction()
        }
    }

    @Synchronized
    fun clear(treeUri: String) {
        val writable = database.writableDatabase
        writable.delete(TABLE, "$COLUMN_TREE_URI = ?", arrayOf(treeUri))
        writable.delete(
            THUMBNAIL_TABLE,
            "$COLUMN_TREE_URI = ?",
            arrayOf(treeUri),
        )
    }

    @Synchronized
    fun getThumbnail(treeUri: String, relativePath: String): ByteArray? {
        val readable = database.writableDatabase
        val bytes = readable.query(
            THUMBNAIL_TABLE,
            arrayOf(COLUMN_THUMBNAIL),
            "$COLUMN_TREE_URI = ? AND $COLUMN_RELATIVE_PATH = ? AND length($COLUMN_THUMBNAIL) <= ?",
            arrayOf(treeUri, relativePath, MAX_THUMBNAIL_BYTES.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            cursor.getBlob(0)
        }
        readable.update(
            THUMBNAIL_TABLE,
            ContentValues().apply { put(COLUMN_LAST_ACCESSED, System.currentTimeMillis()) },
            "$COLUMN_TREE_URI = ? AND $COLUMN_RELATIVE_PATH = ?",
            arrayOf(treeUri, relativePath),
        )
        return bytes
    }

    @Synchronized
    fun putThumbnail(treeUri: String, relativePath: String, bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.size > MAX_THUMBNAIL_BYTES) return
        val writable = database.writableDatabase
        val rowId = writable.insertWithOnConflict(
            THUMBNAIL_TABLE,
            null,
            ContentValues().apply {
                put(COLUMN_TREE_URI, treeUri)
                put(COLUMN_RELATIVE_PATH, relativePath)
                put(COLUMN_THUMBNAIL, bytes)
                put(COLUMN_LAST_ACCESSED, System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        if (rowId == -1L) {
            throw android.database.sqlite.SQLiteException("Private thumbnail cache update failed.")
        }
        while (thumbnailCacheExceedsBounds(writable)) {
            val oldest = writable.query(
                THUMBNAIL_TABLE,
                arrayOf(COLUMN_TREE_URI, COLUMN_RELATIVE_PATH),
                null,
                null,
                null,
                null,
                "$COLUMN_LAST_ACCESSED ASC",
                "1",
            ).use { cursor ->
                if (!cursor.moveToFirst()) return
                cursor.getString(0) to cursor.getString(1)
            }
            writable.delete(
                THUMBNAIL_TABLE,
                "$COLUMN_TREE_URI = ? AND $COLUMN_RELATIVE_PATH = ?",
                arrayOf(oldest.first, oldest.second),
            )
        }
    }

    @Synchronized
    fun removeThumbnail(treeUri: String, relativePath: String) {
        database.writableDatabase.delete(
            THUMBNAIL_TABLE,
            "$COLUMN_TREE_URI = ? AND $COLUMN_RELATIVE_PATH = ?",
            arrayOf(treeUri, relativePath),
        )
    }

    private fun thumbnailCacheExceedsBounds(database: SQLiteDatabase): Boolean =
        database.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(length($COLUMN_THUMBNAIL)), 0) FROM $THUMBNAIL_TABLE",
            null,
        ).use { cursor ->
            cursor.moveToFirst() &&
                (cursor.getLong(0) > MAX_THUMBNAIL_COUNT ||
                    cursor.getLong(1) > MAX_THUMBNAIL_CACHE_BYTES)
        }

    private class Database(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE $TABLE (
                    $COLUMN_TREE_URI TEXT NOT NULL,
                    $COLUMN_RELATIVE_PATH TEXT NOT NULL,
                    $COLUMN_DOCUMENT_ID TEXT NOT NULL,
                    PRIMARY KEY ($COLUMN_TREE_URI, $COLUMN_RELATIVE_PATH)
                ) WITHOUT ROWID
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE $THUMBNAIL_TABLE (
                    $COLUMN_TREE_URI TEXT NOT NULL,
                    $COLUMN_RELATIVE_PATH TEXT NOT NULL,
                    $COLUMN_THUMBNAIL BLOB NOT NULL,
                    $COLUMN_LAST_ACCESSED INTEGER NOT NULL,
                    PRIMARY KEY ($COLUMN_TREE_URI, $COLUMN_RELATIVE_PATH)
                ) WITHOUT ROWID
                """.trimIndent(),
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            database.execSQL("DROP TABLE IF EXISTS $TABLE")
            database.execSQL("DROP TABLE IF EXISTS $THUMBNAIL_TABLE")
            onCreate(database)
        }
    }

    private companion object {
        const val DATABASE_NAME = "kaede-gallery-media-uris.db"
        const val DATABASE_VERSION = 1
        const val TABLE = "media_uri_cache"
        const val COLUMN_TREE_URI = "tree_uri"
        const val COLUMN_RELATIVE_PATH = "relative_path"
        const val COLUMN_DOCUMENT_ID = "document_id"
        const val THUMBNAIL_TABLE = "thumbnail_cache"
        const val COLUMN_THUMBNAIL = "thumbnail"
        const val COLUMN_LAST_ACCESSED = "last_accessed"
        const val MAX_THUMBNAIL_COUNT = 256L
        const val MAX_THUMBNAIL_BYTES = 2 * 1024 * 1024
        const val MAX_THUMBNAIL_CACHE_BYTES = 64L * 1024L * 1024L
    }
}
