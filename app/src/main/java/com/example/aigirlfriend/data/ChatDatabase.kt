package com.example.aigirlfriend.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.aigirlfriend.data.model.ChatHistory

class ChatDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "aigirlfriend.db"
        private const val TABLE_MESSAGES = "chat_messages"

        private const val COL_ID = "id"
        private const val COL_ROLE = "role"
        private const val COL_CONTENT = "content"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_IS_USER = "is_user"
        private const val COL_ATTACHMENT_URI = "attachment_uri"

        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ROLE TEXT NOT NULL,
                $COL_CONTENT TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_USER INTEGER NOT NULL,
                $COL_ATTACHMENT_URI TEXT
            )
        """

        private const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_MESSAGES"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_ATTACHMENT_URI TEXT")
        }
    }

    fun insertMessage(role: String, content: String, timestamp: Long, isUser: Boolean, attachmentUri: String? = null): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ROLE, role)
            put(COL_CONTENT, content)
            put(COL_TIMESTAMP, timestamp)
            put(COL_IS_USER, if (isUser) 1 else 0)
            put(COL_ATTACHMENT_URI, attachmentUri)
        }
        return db.insertOrThrow(TABLE_MESSAGES, null, values)
    }

    fun getAllMessages(): List<ChatHistory> {
        val db = readableDatabase
        val messages = mutableListOf<ChatHistory>()
        db.query(TABLE_MESSAGES, null, null, null, null, null, "$COL_TIMESTAMP ASC").use { cursor ->
            while (cursor.moveToNext()) {
                messages.add(
                    ChatHistory(
                        role = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE)),
                        content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_USER)) == 1,
                        attachmentUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_ATTACHMENT_URI))
                    )
                )
            }
        }
        return messages
    }

    fun clearAllMessages() {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, null, null)
    }
}
