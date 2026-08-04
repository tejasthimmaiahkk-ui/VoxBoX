package me.thimmaiah.voxbox.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryStructureDao {
    @Query("SELECT * FROM folders ORDER BY updatedAt DESC, name COLLATE NOCASE ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM syllabi ORDER BY updatedAt DESC, title COLLATE NOCASE ASC")
    fun observeSyllabi(): Flow<List<SyllabusEntity>>

    @Query("SELECT * FROM note_locations")
    fun observeNoteLocations(): Flow<List<NoteLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSyllabus(syllabus: SyllabusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun placeNote(location: NoteLocationEntity)

    @Query("DELETE FROM note_locations WHERE noteId = :noteId")
    suspend fun removeNoteFromFolder(noteId: String): Int
}
