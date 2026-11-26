package com.splash.offline.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object PhotoStorageHelper {
    private const val MAX_DIM = 1440

    fun saveNormalizedPhoto(context: Context, sourceUri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val orientation = try {
                resolver.openInputStream(sourceUri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Throwable) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var sampleSize = 1
            while (srcW / sampleSize > MAX_DIM || srcH / sampleSize > MAX_DIM) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }

            val upright = if (!matrix.isIdentity) {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val dir = File(context.filesDir, "wall_photos").apply { mkdirs() }
            val outFile = File(dir, "mem_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                upright.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            upright.recycle()
            outFile.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    fun exportPhotoToDeviceGallery(context: Context, filePath: String, title: String): Boolean {
        val srcFile = File(filePath)
        if (!srcFile.exists()) return false
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24).ifBlank { "Recuerdo" }
        val fileName = "Splash_${safeTitle}_${System.currentTimeMillis()}.jpg"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Splash")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        srcFile.inputStream().use { input -> input.copyTo(out) }
                    } ?: return false
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } catch (_: Throwable) {
                    try { resolver.delete(uri, null, null) } catch (_: Throwable) {}
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                val picDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Splash"
                ).apply { mkdirs() }
                val dest = File(picDir, fileName)
                srcFile.inputStream().use { input ->
                    FileOutputStream(dest).use { out -> input.copyTo(out) }
                }
                MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("image/jpeg"), null)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }
}
