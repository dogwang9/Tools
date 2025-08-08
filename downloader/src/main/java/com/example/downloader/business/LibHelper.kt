package com.example.downloader.business

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.downloader.business.model.VideoInfo
import com.example.downloader.business.model.YtDlpException
import com.example.downloader.business.model.YtDlpRequest
import com.example.downloader.business.model.YtDlpResponse
import com.example.downloader.business.utils.FileUtils
import com.example.downloader.business.utils.StreamGobbler
import com.example.downloader.business.utils.StreamProcessExtractor
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.util.Collections

//private const val youtubeDLStableChannelUrl =
//    "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
//private const val youtubeDLNightlyChannelUrl =
//    "https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest"
//private const val youtubeDLMasterChannelUrl =
//    "https://api.github.com/repos/yt-dlp/yt-dlp-master-builds/releases/latest"

/*
        下载网页文件到本地
        FileUtils.copyURLToFile(URL source, File destination, int connectionTimeout, int readTimeout)
              downloadUrl：要下载的资源的 URL
              file：本地目标文件
              5000：连接超时时间，单位是毫秒（ms）
              10000：读取超时时间，单位是毫秒（ms）
         */

/*
.so 文件 是标准的动态链接库，用于运行时依赖（比如 libpython.so、libffmpeg.so、libaria2c.so），它们可以被命令行程序或 Java 层加载；

.zip.so 文件 本质上是被打包的压缩资源（如 Python/FFmpeg 环境），通过代码解压后形成运行目录；

运行这些命令行程序时，需要通过 LD_LIBRARY_PATH 环境变量指明 .so 文件所在的目录，确保正确链接。
 */

object LibHelper {

    private const val TAG = "LibHelper"

    //初始化是否成功
    private var initSuccess = false

    // data/data/<package_name>/no_backup
    private var mNoBackupDir: File? = null

    //jniLibs
    private var mNativeLibraryDir: File? = null

    // ytDlp 二进制文件
    private var mYtDlpFile: File? = null

    //指定动态链接库的搜索路径(当应用程序或Python环境运行时，会从这个路径中寻找需要的动态链接库)
    private var ENV_LD_LIBRARY_PATH: String? = null

    //指定 Python 使用的 SSL 证书路径
    private var ENV_SSL_CERT_FILE: String? = null

    //设置 Python 解释器的工作目录路径
    private var ENV_PYTHON_HOME: String? = null

    //指定临时文件的存储目录
    private var TMPDIR: String = ""

    private val mIdProcessMap = Collections.synchronizedMap(HashMap<String, Process>())

    @Synchronized
    @WorkerThread
    fun init(context: Context) {
        mNoBackupDir = context.noBackupFilesDir
        mNativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        initPython(context)
        initYtDlp(context)
        initFfmpeg()
        initAria2c()

        initSuccess = true
    }

    @WorkerThread
    fun getVideoInfo(request: YtDlpRequest): VideoInfo {
        request.addOption("--dump-json")

        val ytDlpResponse = execute(request)
        val videoInfo: VideoInfo = try {
            ObjectMapper().readValue(ytDlpResponse.out, VideoInfo::class.java)
        } catch (e: IOException) {
            throw YtDlpException("Unable to parse video information", e.toString())
        } ?: throw YtDlpException("Failed error", "failed to fetch video information")

        //用https代替http 防止缩略图无法加载
        videoInfo.thumbnail = videoInfo.thumbnail?.replaceFirst("http://", "https://")
        return videoInfo
    }

    @WorkerThread
    fun downloadVideo(
        url: String,
        processId: String? = null,
        callback: ((Float, String, String) -> Unit)? = null
    ) {
        val request = YtDlpRequest(url)

        request.addOption("--no-mtime")
        //使用 aria2c 作为下载器
        request.addOption("--downloader", "libaria2c.so")
        //将所有文件名缩减为 ASCII 安全字符集（比如把空格换成 _，并移除特殊字符）
        //request.addOption("--restrict-filenames")
        //优先选择分离的视频流（mp4）和音频流（m4a），其次选择最好的 mp4 或任何格式
        //request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
        request.addOption("-o", getDownloadDir() + "/%(title)s.%(ext)s")

        execute(request, processId, callback)
    }

    private fun execute(
        request: YtDlpRequest,
        processId: String? = System.currentTimeMillis().toString(),
        callback: ((Float, String, String) -> Unit)? = null
    ): YtDlpResponse {
        if (!initSuccess) {
            throw YtDlpException("Initialize failed", "please reopen app")
        }
        if (processId != null && mIdProcessMap.containsKey(processId)) {
            throw YtDlpException("Process ID already exists", "please reopen app")
        }
        // 禁用缓存
        if (!request.hasOption("--cache-dir") || request.getOption("--cache-dir") == null) {
            request.addOption("--no-cache-dir")
        }

        if (request.buildCommand().contains("libaria2c.so")) {
            request
                .addOption("--external-downloader-args", "aria2c:--summary-interval=1")
                .addOption(
                    "--external-downloader-args",
                    "aria2c:--ca-certificate=$ENV_SSL_CERT_FILE"
                )
        }

        request.addOption("--ffmpeg-location", File(mNativeLibraryDir, "libffmpeg.so").absolutePath)
        val process: Process
        val exitCode: Int
        val outBuffer = StringBuffer() //stdout
        val errBuffer = StringBuffer() //stderr
        val startTime = System.currentTimeMillis()
        val args = request.buildCommand()
        val command: MutableList<String?> = ArrayList()

        // 相当于执行Python ytdlp ...
        command.addAll(
            listOf(
                File(mNativeLibraryDir, "libpython.so").absolutePath,
                mYtDlpFile!!.absolutePath
            )
        )
        command.addAll(args)
        val processBuilder = ProcessBuilder(command)
        // 配置python的运行环境
        processBuilder.environment().apply {
            this["LD_LIBRARY_PATH"] = ENV_LD_LIBRARY_PATH
            this["SSL_CERT_FILE"] = ENV_SSL_CERT_FILE
            this["PATH"] = System.getenv("PATH") + ":" + mNoBackupDir!!.absolutePath
            this["PYTHONHOME"] = ENV_PYTHON_HOME
            this["HOME"] = ENV_PYTHON_HOME
            this["TMPDIR"] = TMPDIR
        }

        process = try {
            processBuilder.start()
        } catch (e: IOException) {
            throw YtDlpException("Process start error", e.toString())
        }
        if (processId != null) {
            mIdProcessMap[processId] = process
        }
        val outStream = process.inputStream
        val errStream = process.errorStream
        val stdOutProcessor = StreamProcessExtractor(outBuffer, outStream, callback)
        val stdErrProcessor = StreamGobbler(errBuffer, errStream)
        exitCode = try {
            stdOutProcessor.join()
            stdErrProcessor.join()
            process.waitFor()
        } catch (e: InterruptedException) {
            process.destroy()
            if (processId != null) mIdProcessMap.remove(processId)
            throw YtDlpException("Process error", e.toString())
        }
        val out = outBuffer.toString()
        val err = errBuffer.toString()
        if (exitCode > 0) {
            if (processId != null && !mIdProcessMap.containsKey(processId)) {
                throw YtDlpException("Process error", "Process cancel")
            }
            if (!request.hasOption("--dump-json") || out.isEmpty() || !request.hasOption("--ignore-errors")) {
                mIdProcessMap.remove(processId)
                throw YtDlpException("Process error", err)
            }
        }
        mIdProcessMap.remove(processId)
        val elapsedTime = System.currentTimeMillis() - startTime
        return YtDlpResponse(command, exitCode, elapsedTime, out)
    }

    private fun initPython(context: Context) {
        // Python 完整环境的压缩包位置
        val pythonLib = File(mNativeLibraryDir, "libpython.zip.so")
        // Python 完整环境的解压位置
        val pythonDir = File(mNoBackupDir, "python")

        ENV_LD_LIBRARY_PATH = pythonDir.absolutePath + "/usr/lib"
        ENV_SSL_CERT_FILE = pythonDir.absolutePath + "/usr/etc/tls/cert.pem"
        ENV_PYTHON_HOME = pythonDir.absolutePath + "/usr"
        TMPDIR = context.cacheDir.absolutePath

        if (pythonDir.exists()) return
        pythonDir.mkdirs()

        try {
            FileUtils.unzip(pythonLib, pythonDir)
        } catch (e: Exception) {
            initSuccess = false
            FileUtils.deleteFile(pythonDir)
            Log.e(TAG, "initPython: $e")
            throw Exception(e)
        }

    }

    private fun initYtDlp(context: Context) {
        val ytDlpDir = File(mNoBackupDir, "yt_dlp")
        mYtDlpFile = File(ytDlpDir, "ytdlp")

        if (ytDlpDir.exists()) return
        ytDlpDir.mkdirs()

        try {
            val inputStream =
                context.assets.open("ytdlp")
            FileUtils.copyInputStreamToFile(inputStream, mYtDlpFile!!)
        } catch (e: Exception) {
            initSuccess = false
            FileUtils.deleteFile(ytDlpDir)
            Log.e(TAG, "initYtDlp: $e")
            throw Exception(e)
        }
    }

    private fun initFfmpeg() {
        val ffmpegDir = File(mNoBackupDir, "ffmpeg")
        val ffmpegLib = File(mNativeLibraryDir, "libffmpeg.zip.so")

        ENV_LD_LIBRARY_PATH += (":" + ffmpegDir.absolutePath + "/usr/lib")

        if (ffmpegDir.exists()) return
        ffmpegDir.mkdirs()

        try {
            FileUtils.unzip(ffmpegLib, ffmpegDir)
        } catch (e: Exception) {
            initSuccess = false
            FileUtils.deleteFile(ffmpegDir)
            Log.e(TAG, "initFfmpeg: $e")
            throw e
        }
    }

    private fun initAria2c() {
        val aria2cDir = File(mNoBackupDir, "aria2c")
        val aria2cLib = File(mNativeLibraryDir, "libaria2c.zip.so")

        ENV_LD_LIBRARY_PATH += (":" + aria2cDir.absolutePath + "/usr/lib")

        if (aria2cDir.exists()) return
        aria2cDir.mkdirs()

        try {
            FileUtils.unzip(aria2cLib, aria2cDir)
        } catch (e: Exception) {
            initSuccess = false
            FileUtils.deleteFile(aria2cDir)
            Log.e(TAG, "initFfmpeg: $e")
            throw e
        }
    }

    private fun getDownloadDir(): String {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "download_test/temp"
        )
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        return downloadsDir.absolutePath
    }
}