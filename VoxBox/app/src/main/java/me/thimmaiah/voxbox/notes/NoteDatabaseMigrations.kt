package me.thimmaiah.voxbox.notes

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val database = db
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `folders` (
                `id` TEXT NOT NULL,
                `parentId` TEXT,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`parentId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_parentId` ON `folders` (`parentId`)")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_folders_parentId_name` " +
                "ON `folders` (`parentId`, `name`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_locations` (
                `noteId` TEXT NOT NULL,
                `folderId` TEXT NOT NULL,
                PRIMARY KEY(`noteId`),
                FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_note_locations_folderId` " +
                "ON `note_locations` (`folderId`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `syllabi` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `localRelativePath` TEXT NOT NULL,
                `extractedText` TEXT,
                `sha256` TEXT NOT NULL,
                `importedAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_syllabi_sha256` ON `syllabi` (`sha256`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `capture_sessions` (
                `id` TEXT NOT NULL,
                `noteId` TEXT NOT NULL,
                `mode` TEXT NOT NULL,
                `notePolicy` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `syllabusId` TEXT,
                `generatedBlockId` TEXT NOT NULL,
                `revision` INTEGER NOT NULL,
                `lastPatchId` TEXT,
                `frameIntervalMs` INTEGER NOT NULL,
                `changeThreshold` REAL NOT NULL,
                `primarySpeakerId` TEXT,
                `startedAt` INTEGER NOT NULL,
                `stoppedAt` INTEGER,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`syllabusId`) REFERENCES `syllabi`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`generatedBlockId`) REFERENCES `note_blocks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_capture_sessions_noteId` ON `capture_sessions` (`noteId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_capture_sessions_syllabusId` " +
                "ON `capture_sessions` (`syllabusId`)",
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_capture_sessions_generatedBlockId` " +
                "ON `capture_sessions` (`generatedBlockId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_capture_sessions_status` ON `capture_sessions` (`status`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transcript_segments` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `speakerId` TEXT,
                `startMs` INTEGER NOT NULL,
                `endMs` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                `isFinal` INTEGER NOT NULL,
                `confidence` REAL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sessionId`) REFERENCES `capture_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transcript_segments_sessionId` " +
                "ON `transcript_segments` (`sessionId`)",
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_transcript_segments_sessionId_position` " +
                "ON `transcript_segments` (`sessionId`, `position`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `visual_evidence` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `capturedAt` INTEGER NOT NULL,
                `fingerprint` TEXT NOT NULL,
                `deltaScore` REAL NOT NULL,
                `processingState` TEXT NOT NULL,
                `temporaryPath` TEXT,
                `error` TEXT,
                `processedAt` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sessionId`) REFERENCES `capture_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_visual_evidence_sessionId` " +
                "ON `visual_evidence` (`sessionId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_visual_evidence_processingState` " +
                "ON `visual_evidence` (`processingState`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_assets` (
                `id` TEXT NOT NULL,
                `noteId` TEXT NOT NULL,
                `evidenceId` TEXT,
                `kind` TEXT NOT NULL,
                `localRelativePath` TEXT NOT NULL,
                `caption` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`evidenceId`) REFERENCES `visual_evidence`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_note_assets_noteId` ON `note_assets` (`noteId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_note_assets_evidenceId` ON `note_assets` (`evidenceId`)",
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_note_assets_localRelativePath` " +
                "ON `note_assets` (`localRelativePath`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `block_provenance` (
                `blockId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `sourceKind` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `startMs` INTEGER,
                `endMs` INTEGER,
                `confidence` REAL,
                `reviewState` TEXT NOT NULL,
                PRIMARY KEY(`blockId`),
                FOREIGN KEY(`blockId`) REFERENCES `note_blocks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`sessionId`) REFERENCES `capture_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_block_provenance_sessionId` " +
                "ON `block_provenance` (`sessionId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_block_provenance_sourceId` " +
                "ON `block_provenance` (`sourceId`)",
        )
    }
}
