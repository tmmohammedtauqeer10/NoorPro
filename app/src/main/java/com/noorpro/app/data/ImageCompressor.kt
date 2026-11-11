package com.noorpro.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File

/** Downscales a picked photo to <= 1440px on the long side and re-encodes as JPEG 85 (EXIF-rotated).
 *  Returns the original Uri if anything fails, so uploads never break because of compression. */
object ImageCompressor {
    fun compress(context: Context, uri: Uri, maxSide: Int = 1440, quality: Int = 85): Uri = runCatching {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return uri
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return uri
        val rotation = cr.openInputStream(uri)?.use {
            when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        val dir = File(context.cacheDir, "upload_images").apply { mkdirs() }
        val f = File(dir, "img_${System.nanoTime()}.jpg")
        f.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        if (out !== decoded) decoded.recycle()
        out.recycle()
        Uri.fromFile(f)
    }.getOrDefault(uri)
}
