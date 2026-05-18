package com.example.comicaggregator.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LibraryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "comic_library.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_DOWNLOAD_METADATA = "download_metadata"
        private const val TABLE_COMIC_METADATA = "comic_metadata"

        // download_metadata Columns
        private const val COL_DL_ID = "download_id"
        private const val COL_DL_TITLE = "title"
        private const val COL_DL_COVER_URL = "cover_image_url"
        private const val COL_DL_DESCRIPTION = "description"
        private const val COL_DL_DETAIL_URL = "detail_url"

        // comic_metadata Columns
        private const val COL_CM_FILE_NAME = "file_name"
        private const val COL_CM_TITLE = "title"
        private const val COL_CM_COVER_URL = "cover_image_url"
        private const val COL_CM_DESCRIPTION = "description"
        private const val COL_CM_LOCAL_COVER = "local_cover_path"
        private const val COL_CM_DETAIL_URL = "detail_url"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createDlTable = """
            CREATE TABLE $TABLE_DOWNLOAD_METADATA (
                $COL_DL_ID INTEGER PRIMARY KEY,
                $COL_DL_TITLE TEXT,
                $COL_DL_COVER_URL TEXT,
                $COL_DL_DESCRIPTION TEXT,
                $COL_DL_DETAIL_URL TEXT
            )
        """.trimIndent()

        val createCmTable = """
            CREATE TABLE $TABLE_COMIC_METADATA (
                $COL_CM_FILE_NAME TEXT PRIMARY KEY,
                $COL_CM_TITLE TEXT,
                $COL_CM_COVER_URL TEXT,
                $COL_CM_DESCRIPTION TEXT,
                $COL_CM_LOCAL_COVER TEXT,
                $COL_CM_DETAIL_URL TEXT
            )
        """.trimIndent()

        db.execSQL(createDlTable)
        db.execSQL(createCmTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOAD_METADATA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMIC_METADATA")
        onCreate(db)
    }

    fun insertDownloadMetadata(id: Long, title: String, coverUrl: String, description: String, detailUrl: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_DL_ID, id)
            put(COL_DL_TITLE, title)
            put(COL_DL_COVER_URL, coverUrl)
            put(COL_DL_DESCRIPTION, description)
            put(COL_DL_DETAIL_URL, detailUrl)
        }
        db.insertWithOnConflict(TABLE_DOWNLOAD_METADATA, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getDownloadMetadata(id: Long): DownloadMetadata? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DOWNLOAD_METADATA,
            null,
            "$COL_DL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                DownloadMetadata(
                    title = c.getString(c.getColumnIndexOrThrow(COL_DL_TITLE)) ?: "",
                    coverImageUrl = c.getString(c.getColumnIndexOrThrow(COL_DL_COVER_URL)) ?: "",
                    description = c.getString(c.getColumnIndexOrThrow(COL_DL_DESCRIPTION)) ?: "",
                    detailUrl = c.getString(c.getColumnIndexOrThrow(COL_DL_DETAIL_URL)) ?: ""
                )
            } else null
        }
    }

    fun deleteDownloadMetadata(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_DOWNLOAD_METADATA, "$COL_DL_ID = ?", arrayOf(id.toString()))
    }

    fun insertComicMetadata(fileName: String, title: String, coverUrl: String, description: String, localCover: String, detailUrl: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CM_FILE_NAME, fileName)
            put(COL_CM_TITLE, title)
            put(COL_CM_COVER_URL, coverUrl)
            put(COL_CM_DESCRIPTION, description)
            put(COL_CM_LOCAL_COVER, localCover)
            put(COL_CM_DETAIL_URL, detailUrl)
        }
        db.insertWithOnConflict(TABLE_COMIC_METADATA, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getComicMetadata(fileName: String): ComicMetadata? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COMIC_METADATA,
            null,
            "$COL_CM_FILE_NAME = ?",
            arrayOf(fileName),
            null, null, null
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                ComicMetadata(
                    title = c.getString(c.getColumnIndexOrThrow(COL_CM_TITLE)) ?: "",
                    coverImageUrl = c.getString(c.getColumnIndexOrThrow(COL_CM_COVER_URL)) ?: "",
                    description = c.getString(c.getColumnIndexOrThrow(COL_CM_DESCRIPTION)) ?: "",
                    localCoverPath = c.getString(c.getColumnIndexOrThrow(COL_CM_LOCAL_COVER)) ?: "",
                    detailUrl = c.getString(c.getColumnIndexOrThrow(COL_CM_DETAIL_URL)) ?: ""
                )
            } else null
        }
    }
}

data class DownloadMetadata(
    val title: String,
    val coverImageUrl: String,
    val description: String,
    val detailUrl: String
)

data class ComicMetadata(
    val title: String,
    val coverImageUrl: String,
    val description: String,
    val localCoverPath: String,
    val detailUrl: String
)
