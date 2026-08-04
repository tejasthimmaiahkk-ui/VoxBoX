package me.thimmaiah.voxbox.board

import java.io.Closeable
import kotlinx.coroutines.CancellationException

class BoardExtractionCoordinator(
    private val remoteClient: BoardExtractionClient,
    private val offlineClient: BoardExtractionClient,
) : Closeable {
    suspend fun extract(jpegBytes: ByteArray): BoardExtraction {
        return try {
            remoteClient.extract(jpegBytes)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            try {
                val offlineResult = offlineClient.extract(jpegBytes)
                offlineResult.copy(
                    warnings = (
                        listOf("The vision service was unavailable, so bundled offline OCR was used.") +
                            offlineResult.warnings
                        ).distinct(),
                )
            } catch (offlineError: CancellationException) {
                throw offlineError
            } catch (offlineError: Exception) {
                throw BoardExtractionException(
                    "The frame could not be read by the vision service or offline OCR.",
                    offlineError,
                )
            }
        }
    }

    override fun close() {
        (remoteClient as? Closeable)?.close()
        (offlineClient as? Closeable)?.close()
    }
}
