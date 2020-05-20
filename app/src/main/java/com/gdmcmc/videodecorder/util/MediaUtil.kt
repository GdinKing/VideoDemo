package com.gdmcmc.videodecorder.util

import android.R.attr.path
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.IOException


/**
 *
 *
 * @author king
 * @date 2020-03-25 09:00
 */
object MediaUtil {
    //获得音视频的配置器MediaFormat
    fun getFormat(path: String, isVideo: Boolean): MediaFormat? {
        try {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(path)
            val trackCount = mediaExtractor.trackCount
            for (i in 0 until trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(i)
                if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(if (isVideo) "video/" else "audio/")) {
                    return mediaExtractor.getTrackFormat(i)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }


}