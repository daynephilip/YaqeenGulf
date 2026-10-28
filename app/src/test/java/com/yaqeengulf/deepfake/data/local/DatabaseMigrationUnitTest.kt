package com.yaqeengulf.deepfake.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying explicit database migration from schema v1 to v2:
 * 1. Confirms DATABASE_VERSION incremented to 2.
 * 2. Validates explicit ALTER TABLE migration SQL statement and column naming.
 * 3. Confirms no-op behavior for databases already at or above v2.
 * 4. Verifies ScanHistoryEntity entity model retains defaults ('on_device') and accepts 'cloud'.
 */
class DatabaseMigrationUnitTest {

    @Test
    fun testDatabaseVersionIsIncrementedToTwo() {
        assertEquals("Database version must be 2 for Phase 7 migration", 2, AppDatabase.DATABASE_VERSION)
    }

    @Test
    fun testSourceTypeColumnConstant() {
        assertEquals("source_type", AppDatabase.COLUMN_SOURCE_TYPE)
    }

    @Test
    fun testMigrationSqlForV1ToV2() {
        val migrationSql = AppDatabase.getMigrationSql(oldVersion = 1)
        assertNotNull("Migration SQL must not be null for version 1", migrationSql)
        assertTrue(
            "Migration SQL must execute ALTER TABLE scan_history ADD COLUMN source_type",
            migrationSql!!.contains("ALTER TABLE scan_history ADD COLUMN source_type TEXT NOT NULL DEFAULT 'on_device'")
        )
    }

    @Test
    fun testMigrationSqlForV2IsNoOp() {
        val migrationSql = AppDatabase.getMigrationSql(oldVersion = 2)
        assertNull("No migration SQL should run if already on version 2", migrationSql)
    }

    @Test
    fun testScanHistoryEntityDefaultSourceTypeIsOnDevice() {
        val entity = ScanHistoryEntity(
            modality = "IMAGE",
            verdict = "SYNTHETIC",
            confidenceScore = 0.94f,
            modelUsed = "EfficientNet-B3",
            summary = "Synthetic patterns detected",
            sourceIdentifier = "photo.jpg"
        )

        assertEquals("on_device", entity.sourceType)
    }

    @Test
    fun testScanHistoryEntityExplicitCloudSourceType() {
        val entity = ScanHistoryEntity(
            modality = "URL_CHECK",
            verdict = "RELIABLE",
            confidenceScore = 0.91f,
            modelUsed = "Google Gemini",
            summary = "Established regional news source",
            sourceIdentifier = "https://reuters.com/news",
            sourceType = "cloud"
        )

        assertEquals("cloud", entity.sourceType)
        assertEquals("URL_CHECK", entity.modality)
    }
}
