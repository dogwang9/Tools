package com.example.downloader.business.utils

import android.system.Os
import android.util.Log
import androidx.annotation.WorkerThread
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

object FileUtils {

    private const val TAG = "FileUtils"

    @WorkerThread
    fun deleteFile(file: File) {
        if (!file.exists()) {
            return
        }
        if (file.isFile()) {
            file.delete()
        }
        if (file.isDirectory()) {
            deleteDirectory(file)
        }
    }

    @WorkerThread
    fun unzip(sourceFile: File?, targetDirectory: File) {
        ZipFile(sourceFile).use { zipFile ->
            val entries = zipFile.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryDestination = File(targetDirectory, entry.name)
                if (!entryDestination.canonicalPath.startsWith(targetDirectory.canonicalPath + File.separator)) {
                    throw IllegalAccessException("Entry is outside of the target dir: " + entry.name)
                }
                if (entry.isDirectory) {
                    entryDestination.mkdirs()
                } else if (entry.isUnixSymlink) {
                    zipFile.getInputStream(entry).use { `in` ->
                        val symlink = IOUtils.toString(`in`, StandardCharsets.UTF_8)
                        Os.symlink(symlink, entryDestination.absolutePath)
                    }
                } else {
                    entryDestination.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { `in` ->
                        FileOutputStream(entryDestination).use { out ->
                            IOUtils.copy(
                                `in`,
                                out
                            )
                        }
                    }
                }
            }
        }
    }

    @WorkerThread
    fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } != -1) {
                outputStream.write(buffer, 0, length)
            }
        } catch (e: IOException) {
            Log.e(TAG, "copyInputStreamToFile copy error : $e")
        } finally {
            try {
                inputStream.close()
                outputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "copyInputStreamToFile close error : $e")
            }
        }
    }

    @WorkerThread
    fun copy(source: File, dest: File) {
        // 创建目标文件夹（如果不存在）
        dest.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        // 创建目标文件（如果不存在）
        if (!dest.exists()) {
            dest.createNewFile()
        }

        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        }
    }

    private fun deleteDirectory(directory: File) {
        if (!directory.exists()) return

        directory.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteDirectory(child)
            } else {
                child.delete()
            }
        }
        directory.delete()
    }
}