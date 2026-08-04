package me.thimmaiah.voxbox.notes

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import me.thimmaiah.voxbox.session.requireValidRelativePath

interface LibraryStructureRepository {
    fun observeFolders(): Flow<List<FolderEntity>>
    fun observeSyllabi(): Flow<List<SyllabusEntity>>
    fun observeNoteLocations(): Flow<List<NoteLocationEntity>>
    suspend fun createFolder(name: String, parentId: String? = null): FolderEntity
    suspend fun addSyllabus(
        title: String,
        localRelativePath: String,
        sha256: String,
        extractedText: String? = null,
    ): SyllabusEntity
    suspend fun placeNote(noteId: String, folderId: String)
    suspend fun removeNoteFromFolder(noteId: String): Boolean
}

class RoomLibraryStructureRepository(
    private val dao: LibraryStructureDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : LibraryStructureRepository {
    override fun observeFolders(): Flow<List<FolderEntity>> = dao.observeFolders()

    override fun observeSyllabi(): Flow<List<SyllabusEntity>> = dao.observeSyllabi()

    override fun observeNoteLocations(): Flow<List<NoteLocationEntity>> = dao.observeNoteLocations()

    override suspend fun createFolder(name: String, parentId: String?): FolderEntity {
        val now = clock()
        val folder = FolderEntity(
            id = idFactory(),
            parentId = parentId?.trim()?.takeIf(String::isNotBlank),
            name = name.trim().also {
                require(it.isNotBlank()) { "Folder name cannot be blank." }
            },
            createdAt = now,
            updatedAt = now,
        )
        dao.insertFolder(folder)
        return folder
    }

    override suspend fun addSyllabus(
        title: String,
        localRelativePath: String,
        sha256: String,
        extractedText: String?,
    ): SyllabusEntity {
        requireValidRelativePath(localRelativePath)
        val normalizedHash = sha256.trim().lowercase()
        require(SHA_256.matches(normalizedHash)) { "Syllabus hash must be a SHA-256 value." }
        val now = clock()
        val syllabus = SyllabusEntity(
            id = idFactory(),
            title = title.trim().also {
                require(it.isNotBlank()) { "Syllabus title cannot be blank." }
            },
            localRelativePath = localRelativePath.replace('\\', '/').trim(),
            extractedText = extractedText?.trim()?.takeIf(String::isNotBlank),
            sha256 = normalizedHash,
            importedAt = now,
            updatedAt = now,
        )
        dao.insertSyllabus(syllabus)
        return syllabus
    }

    override suspend fun placeNote(noteId: String, folderId: String) {
        require(noteId.isNotBlank()) { "Note id cannot be blank." }
        require(folderId.isNotBlank()) { "Folder id cannot be blank." }
        dao.placeNote(NoteLocationEntity(noteId.trim(), folderId.trim()))
    }

    override suspend fun removeNoteFromFolder(noteId: String): Boolean {
        return dao.removeNoteFromFolder(noteId) == 1
    }

    private companion object {
        val SHA_256 = Regex("^[0-9a-f]{64}$")
    }
}
