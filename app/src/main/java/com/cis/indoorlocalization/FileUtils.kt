package com.cis.indoorlocalization

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    fun archiveMap(context: Context) {
        val currentMapPath = File(context.filesDir, "currentMap")
        val titleFile = File(currentMapPath, "title.txt")
        if (!titleFile.exists()) {
            Log.e("FileUtils", "title.txt not found")
            return
        }

        val mapTitle = titleFile.readText().trim()
        val otherMapsPath = File(context.filesDir, "otherMaps")
        if (!otherMapsPath.exists()) {
            otherMapsPath.mkdirs()
        }

        val zipFilePath = File(otherMapsPath, "$mapTitle.zip")
        ZipOutputStream(FileOutputStream(zipFilePath)).use { zos ->
            currentMapPath.listFiles()?.forEach { file ->
                FileInputStream(file).use { fis ->
                    val zipEntry = ZipEntry(file.name)
                    zos.putNextEntry(zipEntry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    fun importMapFromExternalStorage(context: Context, uri: Uri): Boolean {
        val tempDir = File(context.filesDir, "temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val inputStream = context.contentResolver.openInputStream(uri) ?: return false
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(tempDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }

        val titleFile = File(tempDir, "title.txt")
        val imageFile = File(tempDir, "croppedImage.jpg")
        val markersFile = File(tempDir, "markers.csv")

        val isValid = titleFile.exists() && imageFile.exists() && markersFile.exists() && validateMarkersFile(markersFile)
        tempDir.deleteRecursively()
        return isValid
    }

    fun unzipMap(context: Context, zipFile: File, targetFolderName: String) {
        val targetDir = File(context.filesDir, targetFolderName)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        } else {
            targetDir.deleteRecursively()
            targetDir.mkdirs()
        }

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun validateMarkersFile(file: File): Boolean {
        return try {
            BufferedReader(FileReader(file)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size != 2 || parts.any { it.toFloatOrNull() == null }) {
                        return false
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
