package com.cis.indoorlocalization

import android.content.Context
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.os.Environment


object FileUtils {

    // Existing functions...

    fun archiveMap(context: Context, mapTitle: String) {
        val currentMapPath = File(context.filesDir, "currentMap")
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

    fun exportMapToExternalStorage(context: Context, mapTitle: String): Boolean {
        val otherMapsPath = File(context.filesDir, "otherMaps")
        val zipFile = File(otherMapsPath, "$mapTitle.zip")
        if (!zipFile.exists()) {
            return false
        }

        val externalPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val externalFile = File(externalPath, zipFile.name)
        FileInputStream(zipFile).use { input ->
            FileOutputStream(externalFile).use { output ->
                input.copyTo(output)
            }
        }
        return true
    }

    fun importMapFromExternalStorage(context: Context, zipFilePath: String): Boolean {
        val zipFile = File(zipFilePath)
        if (!zipFile.exists() || !zipFile.extension.equals("zip", ignoreCase = true)) {
            return false
        }

        val tempDir = File(context.filesDir, "temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
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
