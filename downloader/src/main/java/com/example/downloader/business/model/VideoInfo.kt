package com.example.downloader.business.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class VideoInfo {
    val id: String? = null
    val fullTitle: String? = null //完整标题
    val title: String? = null //简略标题

    @JsonProperty("upload_date")
    val uploadDate: String? = null //上传日期

    @JsonProperty("display_id")
    val displayId: String? = null
    val duration = 0 //时长
    val description: String? = null //描述
    var thumbnail: String? = null //缩略图
    val license: String? = null //授权信息
    val extractor: String? = null //用于提取信息的工具来源

    @JsonProperty("extractor_key")
    val extractorKey: String? = null //用于提取信息的工具来源的key

    @JsonProperty("view_count")
    val viewCount: String? = null //播放次数

    @JsonProperty("like_count")
    val likeCount: String? = null //点赞数

    @JsonProperty("dislike_count")
    val dislikeCount: String? = null //不喜欢数

    @JsonProperty("repost_count")
    val repostCount: String? = null //分享数

    @JsonProperty("average_rating")
    val averageRating: String? = null //平均评分

    @JsonProperty("uploader_id")
    val uploaderId: String? = null //上传者id
    val uploader: String? = null //上传者

    @JsonProperty("player_url")
    val playerUrl: String? = null //播放器url

    @JsonProperty("webpage_url")
    val webpageUrl: String? = null //网页url

    @JsonProperty("webpage_url_basename")
    val webpageUrlBasename: String? = null
    val resolution: String? = null //分辨率
    val width = 0 //宽
    val height = 0 //高
    val format: String? = null //格式

    @JsonProperty("format_id")
    val formatId: String? = null //格式id
    val ext: String? = null //文件扩展名

    @JsonProperty("filesize")
    val fileSize: Long = 0 //文件大小

    @JsonProperty("filesize_approx")
    val fileSizeApproximate: Long = 0 //近似文件大小

    @JsonProperty("http_headers")
    val httpHeaders: Map<String, String>? = null
    val categories: ArrayList<String>? = null
    val tags: ArrayList<String>? = null

    @JsonProperty("requested_formats")
    val requestedFormats: ArrayList<VideoFormat>? = null
    val formats: ArrayList<VideoFormat>? = null //不同格式的音视频集合
    val thumbnails: ArrayList<VideoThumbnail>? = null //缩略图

    @JsonProperty("manifest_url")
    val manifestUrl: String? = null

    //如果fileSize和fileSizeApproximate都为0，那么只能采用比特率和时长来进行近似换算
    //需要考虑到有些来源的视频requestedFormats为空的情况
    fun getSize(): Long {
        return requestedFormats?.sumOf { format ->
            format.getSize(duration)
        } ?: formats?.find { it.formatId == formatId }?.getSize(duration) ?: 0L
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class VideoFormat {
    val asr = 0 //音频采样率
    val tbr = 0 //总比特率
    val abr = 0 //音频比特率
    val format: String? = null //格式

    @JsonProperty("format_id")
    val formatId: String? = null //格式id

    @JsonProperty("format_note")
    val formatNote: String? = null
    val ext: String? = null //文件扩展名
    val preference = 0 //格式优先级
    val vcodec: String? = null //视频编解码器的名称
    val acodec: String? = null //音频编解码器的名称
    val width = 0
    val height = 0

    @JsonProperty("filesize")
    val fileSize: Long = 0  //文件大小

    @JsonProperty("filesize_approx")
    val fileSizeApproximate: Long = 0 //文件大小近似值
    val fps = 0 //帧
    val url: String? = null //url

    @JsonProperty("manifest_url")
    val manifestUrl: String? = null

    @JsonProperty("http_headers")
    val httpHeaders: Map<String, String>? = null

    //如果fileSize和fileSizeApproximate都为0，那么只能采用比特率和时长来进行近似换算
    fun getSize(duration: Int): Long {
        return when {
            fileSize != 0L -> fileSize
            fileSizeApproximate != 0L -> fileSizeApproximate
            else -> (duration * tbr * 125).toLong()
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class VideoThumbnail {
    val url: String? = null
    val id: String? = null
}