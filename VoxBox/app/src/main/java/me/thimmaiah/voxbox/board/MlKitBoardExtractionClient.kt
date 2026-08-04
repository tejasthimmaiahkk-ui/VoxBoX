package me.thimmaiah.voxbox.board

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayInputStream
import java.io.Closeable
import kotlin.math.max
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitBoardExtractionClient(
    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
) : BoardExtractionClient, Closeable {
    override suspend fun extract(jpegBytes: ByteArray): BoardExtraction {
        if (jpegBytes.isEmpty()) {
            throw BoardExtractionException("The captured frame was empty.")
        }
        val bitmap = decodeOcrBitmap(jpegBytes)
        return try {
            val recognized = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
            recognized.toOfflineExtraction()
        } catch (error: CancellationException) {
            throw error
        } catch (error: BoardExtractionException) {
            throw error
        } catch (error: Exception) {
            throw BoardExtractionException("Offline text recognition could not read this frame.", error)
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() {
        recognizer.close()
    }
}

private const val MAX_OCR_IMAGE_DIMENSION = 2_560

private fun decodeOcrBitmap(jpegBytes: ByteArray): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw BoardExtractionException("The captured JPEG could not be decoded.")
    }
    val decoded = BitmapFactory.decodeByteArray(
        jpegBytes,
        0,
        jpegBytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = calculateOcrSampleSize(bounds.outWidth, bounds.outHeight)
        },
    ) ?: throw BoardExtractionException("The captured JPEG could not be decoded.")
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(jpegBytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    return decoded.applyExifOrientation(orientation)
}

internal fun calculateOcrSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int = MAX_OCR_IMAGE_DIMENSION,
): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
    var sampleSize = 1
    while (max(width, height) / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return this
    }
    val transformed = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (transformed !== this) recycle()
    return transformed
}

internal fun Text.toOfflineExtraction(): BoardExtraction {
    val visibleText = text.trim()
    val lines = visibleText.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    val warnings = buildList {
        add("Bundled offline OCR extracts visible text only; review the summary and concepts before saving.")
        add("Bundled offline OCR does not report a confidence score.")
        if (visibleText.isBlank()) {
            add("No legible board text was detected. Try moving closer and reducing glare.")
        }
    }
    return BoardExtraction(
        title = lines.firstOrNull()?.take(100).orEmpty().ifBlank { "Board capture" },
        summary = "",
        visibleText = visibleText,
        concepts = emptyList(),
        confidence = 0.0,
        warnings = warnings,
        source = BoardExtractionSource.OFFLINE_OCR,
    )
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel(CancellationException("ML Kit text recognition was cancelled."))
    }
}
