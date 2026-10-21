package com.grimsqueaker.comicpulse.util

import android.content.Context
import android.net.Uri
import net.sf.sevenzipjbinding.SevenZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Utility object for safely extracting comic archives (.cbz, .cbr) to a temporary cache.
 * Leverages native 7-Zip-JBinding engine to support all legacy RAR4 and modern RAR5 CBR formats.
 */
object ComicExtractor {

    /**
     * Extracts a comic archive into a unique cache directory and returns a sorted list of image paths.
     * @param context Active Android context to access ContentResolver and CacheDir.
     * @param uri The URI of the target comic file.
     * @param isCbr True if the file is a .cbr archive, false for .cbz.
     */
    suspend fun extractComic(context: Context, uri: Uri, isCbr: Boolean): List<File> = withContext(Dispatchers.IO) {
        val uniqueFolderName = uri.hashCode().toString()
        val extractDir = File(context.cacheDir, "extracted_comics/$uniqueFolderName")
        
        // If already extracted, just return the cached files
        if (extractDir.exists() && extractDir.listFiles()?.isNotEmpty() == true) {
            return@withContext getSortedImages(extractDir)
        }

        // Validate free storage space for large files (require at least 2.2x file size)
        val fileSize = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (e: Exception) {
            0L
        }
        val freeSpace = context.cacheDir.freeSpace
        if (fileSize > 0L && freeSpace < (fileSize * 2.2).toLong()) {
            throw java.io.IOException("Insufficient space in cache directory. File size: ${fileSize / (1024 * 1024)}MB. Required free space: ${(fileSize * 2.2).toLong() / (1024 * 1024)}MB. Available free space: ${freeSpace / (1024 * 1024)}MB.")
        }

        extractDir.mkdirs()

        try {
            if (isCbr) {
                extractCbr(context, uri, extractDir)
            } else {
                try {
                    extractCbz(context, uri, extractDir)
                } catch (e: Exception) {
                    // Fallback to CBR if CBZ extraction fails (e.g. malformed zip, or actually a RAR file renamed to .cbz)
                    extractCbr(context, uri, extractDir)
                }
            }
        } catch (e: Exception) {
            // Clean up corrupted extraction
            extractDir.deleteRecursively()
            throw e
        }

        return@withContext getSortedImages(extractDir)
    }

    private fun extractCbz(context: Context, uri: Uri, targetDir: File) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("Cannot open URI")
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // Ignore directories implicitly or explicitly marked
                if (!entry.isDirectory && !entry.name.endsWith("/") && isImageFile(entry.name)) {
                    val safeName = File(entry.name).name
                    if (safeName.isNotBlank()) {
                        val outFile = File(targetDir, safeName)
                        if (!outFile.isDirectory) {
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun extractCbr(context: Context, uri: Uri, targetDir: File) {
        // 7-Zip-JBinding requires a RandomAccessFile, which needs a native File path.
        // We will copy the ContentResolver stream to a temporary native File first.
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("Cannot open URI")
        val tempRar = File(context.cacheDir, "temp_cbr_${System.currentTimeMillis()}.cbr")
        
        try {
            FileOutputStream(tempRar).use { fos ->
                inputStream.copyTo(fos)
            }

            SevenZip.initSevenZipFromPlatformJAR()
            val raf = java.io.RandomAccessFile(tempRar, "r")
            val archive = SevenZip.openInArchive(null, net.sf.sevenzipjbinding.impl.RandomAccessFileInStream(raf))
            val simpleArchive = archive.simpleInterface
            
            for (item in simpleArchive.archiveItems) {
                if (!item.isFolder) {
                    val fileName = item.path ?: continue
                    if (isImageFile(fileName)) {
                        val safeName = File(fileName).name
                        val outFile = File(targetDir, safeName)
                        val fos = FileOutputStream(outFile)
                        try {
                            item.extractSlow { data ->
                                fos.write(data)
                                data.size
                            }
                        } finally {
                            fos.close()
                        }
                    }
                }
            }
            archive.close()
            raf.close()
        } finally {
            if (tempRar.exists()) tempRar.delete() // Clean up temp RAR file
        }
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun getSortedImages(dir: File): List<File> {
        val files = dir.listFiles()?.toList() ?: emptyList()
        // Sort alphabetically, which generally orders sequential comic pages correctly (e.g. 001.jpg, 002.jpg)
        return files.filter { isImageFile(it.name) }.sortedBy { it.name }
    }

    /**
     * Extracts only the very first image of a comic archive to serve as a fast cover preview thumbnail.
     */
    suspend fun getOrCreateCover(context: Context, uri: Uri, isCbr: Boolean): File? = withContext(Dispatchers.IO) {
        val uniqueName = uri.hashCode().toString()
        val coverFile = File(context.cacheDir, "covers/$uniqueName.jpg")
        
        if (coverFile.exists() && coverFile.length() > 0) {
            return@withContext coverFile
        }
        
        coverFile.parentFile?.mkdirs()
        
        try {
            if (isCbr) {
                extractFirstImageCbr(context, uri, coverFile)
            } else {
                try {
                    extractFirstImageCbz(context, uri, coverFile)
                } catch (e: Exception) {
                    extractFirstImageCbr(context, uri, coverFile)
                }
            }
            if (coverFile.exists() && coverFile.length() > 0) {
                return@withContext coverFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun extractFirstImageCbz(context: Context, uri: Uri, destFile: File) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && !entry.name.endsWith("/") && isImageFile(entry.name)) {
                    FileOutputStream(destFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    return
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun extractFirstImageCbr(context: Context, uri: Uri, destFile: File) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val tempRar = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}.cbr")
        try {
            FileOutputStream(tempRar).use { fos ->
                inputStream.copyTo(fos)
            }
            
            SevenZip.initSevenZipFromPlatformJAR()
            val raf = java.io.RandomAccessFile(tempRar, "r")
            val archive = SevenZip.openInArchive(null, net.sf.sevenzipjbinding.impl.RandomAccessFileInStream(raf))
            val simpleArchive = archive.simpleInterface
            for (item in simpleArchive.archiveItems) {
                if (!item.isFolder) {
                    val fileName = item.path ?: continue
                    if (isImageFile(fileName)) {
                        val fos = FileOutputStream(destFile)
                        try {
                            item.extractSlow { data ->
                                fos.write(data)
                                data.size
                            }
                        } finally {
                            fos.close()
                        }
                        archive.close()
                        raf.close()
                        return
                    }
                }
            }
            archive.close()
            raf.close()
        } finally {
            if (tempRar.exists()) tempRar.delete()
        }
    }

    /**
     * Fast-extracts the first N pages from a comic archive to serve as covers/previews.
     */
    suspend fun getPreviewPages(context: Context, uri: Uri, isCbr: Boolean, limit: Int = 15): List<File> = withContext(Dispatchers.IO) {
        val uniqueFolderName = uri.hashCode().toString()
        val targetDir = File(context.cacheDir, "previews/$uniqueFolderName")
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            return@withContext getSortedImages(targetDir).take(limit)
        }
        targetDir.mkdirs()
        
        try {
            if (isCbr) {
                SevenZip.initSevenZipFromPlatformJAR()
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
                val tempRar = File(context.cacheDir, "temp_preview_${System.currentTimeMillis()}.cbr")
                try {
                    FileOutputStream(tempRar).use { fos ->
                        inputStream.copyTo(fos)
                    }
                    val raf = java.io.RandomAccessFile(tempRar, "r")
                    val archive = SevenZip.openInArchive(null, net.sf.sevenzipjbinding.impl.RandomAccessFileInStream(raf))
                    val simpleArchive = archive.simpleInterface
                    var count = 0
                    for (item in simpleArchive.archiveItems) {
                        if (!item.isFolder) {
                            val fileName = item.path ?: continue
                            if (isImageFile(fileName)) {
                                val safeName = File(fileName).name
                                val outFile = File(targetDir, safeName)
                                val fos = FileOutputStream(outFile)
                                try {
                                    item.extractSlow { data ->
                                        fos.write(data)
                                        data.size
                                    }
                                } finally {
                                    fos.close()
                                }
                                count++
                                if (count >= limit) break
                            }
                        }
                    }
                    archive.close()
                    raf.close()
                } finally {
                    if (tempRar.exists()) tempRar.delete()
                }
            } else {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    var count = 0
                    while (entry != null) {
                        if (!entry.isDirectory && !entry.name.endsWith("/") && isImageFile(entry.name)) {
                            val safeName = File(entry.name).name
                            if (safeName.isNotBlank()) {
                                val outFile = File(targetDir, safeName)
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                count++
                                if (count >= limit) break
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext getSortedImages(targetDir).take(limit)
    }
}

