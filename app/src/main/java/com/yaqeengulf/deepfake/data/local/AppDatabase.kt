package com.yaqeengulf.deepfake.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Data Access Object interface for scan history.
 */
interface ScanHistoryDao {
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>
    suspend fun insertScan(scan: ScanHistoryEntity): Long
    suspend fun deleteScan(id: Long): Int
    suspend fun clearAll()
}

/**
 * AppDatabase providing thread-safe SQLite persistence for scan records.
 * Provides Flow-based observation matching Room DAO patterns.
 */
class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), ScanHistoryDao {

    private val _historyFlow = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())

    init {
        refreshHistory()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MODALITY TEXT NOT NULL,
                $COLUMN_VERDICT TEXT NOT NULL,
                $COLUMN_CONFIDENCE REAL NOT NULL,
                $COLUMN_MODEL TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SUMMARY TEXT NOT NULL,
                $COLUMN_SOURCE TEXT,
                $COLUMN_SOURCE_TYPE TEXT NOT NULL DEFAULT 'on_device'
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        getMigrationSql(oldVersion)?.let { sql ->
            db.execSQL(sql)
        }
    }

    override fun getAllHistory(): Flow<List<ScanHistoryEntity>> = _historyFlow.asStateFlow()

    override suspend fun insertScan(scan: ScanHistoryEntity): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MODALITY, scan.modality)
            put(COLUMN_VERDICT, scan.verdict)
            put(COLUMN_CONFIDENCE, scan.confidenceScore)
            put(COLUMN_MODEL, scan.modelUsed)
            put(COLUMN_TIMESTAMP, scan.timestamp)
            put(COLUMN_SUMMARY, scan.summary)
            put(COLUMN_SOURCE, scan.sourceIdentifier)
            put(COLUMN_SOURCE_TYPE, scan.sourceType)
        }
        val id = db.insert(TABLE_NAME, null, values)
        refreshHistory()
        id
    }

    override suspend fun deleteScan(id: Long): Int = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val rows = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        refreshHistory()
        rows
    }

    override suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
        refreshHistory()
    }

    private fun refreshHistory() {
        try {
            val db = readableDatabase
            val cursor: Cursor = db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                "$COLUMN_TIMESTAMP DESC"
            )
            val list = mutableListOf<ScanHistoryEntity>()
            cursor.use { c ->
                val idCol = c.getColumnIndexOrThrow(COLUMN_ID)
                val modCol = c.getColumnIndexOrThrow(COLUMN_MODALITY)
                val verCol = c.getColumnIndexOrThrow(COLUMN_VERDICT)
                val confCol = c.getColumnIndexOrThrow(COLUMN_CONFIDENCE)
                val modelCol = c.getColumnIndexOrThrow(COLUMN_MODEL)
                val timeCol = c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
                val sumCol = c.getColumnIndexOrThrow(COLUMN_SUMMARY)
                val srcCol = c.getColumnIndexOrThrow(COLUMN_SOURCE)
                val srcTypeCol = c.getColumnIndex(COLUMN_SOURCE_TYPE)

                while (c.moveToNext()) {
                    val srcType = if (srcTypeCol >= 0 && !c.isNull(srcTypeCol)) {
                        c.getString(srcTypeCol)
                    } else {
                        "on_device"
                    }

                    list.add(
                        ScanHistoryEntity(
                            id = c.getLong(idCol),
                            modality = c.getString(modCol),
                            verdict = c.getString(verCol),
                            confidenceScore = c.getFloat(confCol),
                            modelUsed = c.getString(modelCol),
                            timestamp = c.getLong(timeCol),
                            summary = c.getString(sumCol),
                            sourceIdentifier = c.getString(srcCol),
                            sourceType = srcType
                        )
                    )
                }
            }
            _historyFlow.value = list
        } catch (e: Exception) {
            _historyFlow.value = emptyList()
        }
    }

    companion object {
        const val DATABASE_NAME = "yaqeen_gulf.db"
        const val DATABASE_VERSION = 2

        const val TABLE_NAME = "scan_history"
        const val COLUMN_ID = "id"
        const val COLUMN_MODALITY = "modality"
        const val COLUMN_VERDICT = "verdict"
        const val COLUMN_CONFIDENCE = "confidence"
        const val COLUMN_MODEL = "model"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SUMMARY = "summary"
        const val COLUMN_SOURCE = "source"
        const val COLUMN_SOURCE_TYPE = "source_type"

        fun getMigrationSql(oldVersion: Int): String? {
            return if (oldVersion < 2) {
                "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_SOURCE_TYPE TEXT NOT NULL DEFAULT 'on_device'"
            } else {
                null
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
