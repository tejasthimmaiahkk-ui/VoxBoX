package me.thimmaiah.voxbox.board

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.floor

data class PixelCropBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

data class StoredDiagram(
    val relativePath: String,
    val caption: String,
    val width: Int,
    val height: Int,
)

class DiagramCropper(
    private val filesDir: File,
) {
    fun cropAndStore(
        jpegBytes: ByteArray,
        region: DiagramRegion,
        noteId: String,
        assetId: String,
    ): StoredDiagram {
        require(region.isValid()) { "Diagram crop must use valid normalized coordinates." }
        val bitmap = decodeOrientedBitmap(jpegBytes)
        return try {
            val bounds = normalizedCropBounds(bitmap.width, bitmap.height, region)
            val crop = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width, bounds.height)
            try {
                val safeNoteId = safePathPart(noteId)
                val safeAssetId = safePathPart(assetId)
                val directory = File(filesDir, "note-assets/$safeNoteId").apply {
                    if (!exists() && !mkdirs()) throw BoardExtractionException("The diagram asset folder could not be created.")
                }
                val target = File(directory, "$safeAssetId.jpg")
                val temporary = File(directory, ".$safeAssetId.tmp")
                try {
                    FileOutputStream(temporary).use { output ->
                        if (!crop.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                            throw BoardExtractionException("The diagram crop could not be encoded.")
                        }
                        output.fd.sync()
                    }
                    if (target.exists() && !target.delete()) {
                        throw BoardExtractionException("The previous diagram asset could not be replaced.")
                    }
                    if (!temporary.renameTo(target)) {
                        throw BoardExtractionException("The diagram crop could not be committed.")
                    }
                } catch (error: Exception) {
                    temporary.delete()
                    throw error
                }
                StoredDiagram(
                    relativePath = "note-assets/$safeNoteId/$safeAssetId.jpg",
                    caption = region.caption.trim().ifBlank { "Captured board diagram" },
                    width = crop.width,
                    height = crop.height,
                )
            } finally {
                crop.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }
}

internal fun normalizedCropBounds(
    imageWidth: Int,
    imageHeight: Int,
    region: DiagramRegion,
): PixelCropBounds {
    require(imageWidth > 0 && imageHeight > 0)
    require(region.isValid())
    val left = floor(region.left * imageWidth).toInt().coerceIn(0, imageWidth - 1)
    val top = floor(region.top * imageHeight).toInt().coerceIn(0, imageHeight - 1)
    val right = ceil((region.left + region.width) * imageWidth).toInt().coerceIn(left + 1, imageWidth)
    val bottom = ceil((region.top + region.height) * imageHeight).toInt().coerceIn(top + 1, imageHeight)
    return PixelCropBounds(left, top, right - left, bottom - top)
}

private fun safePathPart(value: String): String {
    val sanitized = value.trim().replace(Regex("[^A-Za-z0-9_-]"), "_").take(96)
    require(sanitized.isNotBlank()) { "Asset path identifiers cannot be blank." }
    return sanitized
}

private fun decodeOrientedBitmap(jpegBytes: ByteArray): Bitmap {
    if (jpegBytes.isEmpty()) throw BoardExtractionException("The captured frame was empty.")
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        ?: throw BoardExtractionException("The captured frame could not be decoded for diagram cropping.")
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(jpegBytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
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
        else -> return bitmap
    }
    val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (oriented !== bitmap) bitmap.recycle()
    return oriented
}
