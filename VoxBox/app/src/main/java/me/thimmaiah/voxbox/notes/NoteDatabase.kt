package me.thimmaiah.voxbox.notes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        NoteBlockEntity::class,
        FolderEntity::class,
        NoteLocationEntity::class,
        SyllabusEntity::class,
        CaptureSessionEntity::class,
        TranscriptSegmentEntity::class,
        VisualEvidenceEntity::class,
        NoteAssetEntity::class,
        BlockProvenanceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun libraryStructureDao(): LibraryStructureDao
    abstract fun captureSessionDao(): CaptureSessionDao

    companion object {
        @Volatile
        private var instance: NoteDatabase? = null

        fun get(context: Context): NoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NoteDatabase::class.java,
                "voxbox-notes.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
