package com.gdmcmc.videodecorder.activity

import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gdmcmc.videodecorder.R
import kotlinx.android.synthetic.main.activity_media_extractor.*


/**
 * MediaExtractor使用示例
 * @author king
 * @time 2020-5-6 15:55
 */
class MediaExtractorActivity : AppCompatActivity() {
    //媒体提取器
    private var mExtractor: MediaExtractor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_extractor)
        init()
    }

    private fun init() {
        try {
            //构造
            mExtractor = MediaExtractor()
            //设置视频数据源，可以是本地文件也可以是网络文件，这里我使用了手机本机拍摄的示例文件
            mExtractor?.setDataSource("${externalCacheDir!!.absolutePath}/test.mp4")
        } catch (e: Exception) {
            Log.e("Test", "出错了", e)
        }

    }

    fun getMediaInfo(view: View) {
        var content = ""
        for (i in 0 until mExtractor!!.getTrackCount()) {
            //获取轨道信息
            val mediaFormat: MediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.i("Test", "第${i}个轨道格式:$mime\n")
            content += "第${i}个轨道格式:$mime\n"
        }
        getVideoInfo()
        tv_info.text = content
    }

    /**
     * 获取视频信息
     */
    private fun getVideoInfo(){
        val mediaFormat: MediaFormat = mExtractor!!.getTrackFormat(getTrackIndex("video/"))
        val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH) //获取高度
        val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT) //获取高度
        val duration = mediaFormat.getLong(MediaFormat.KEY_DURATION) //总时间
        val maxInputSize =
            mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) //获取视频缓存输出的最大大小

        Log.i("Test","视频宽度：${width}")
        Log.i("Test","视频高度：${height}")
        Log.i("Test","视频时长：${duration}")
        Log.i("Test","缓存最大大小：${maxInputSize}")
        try {
            //以下是不一定能获取到的数据
            val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) //获取采样率
            val bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE) //获取码率
            val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) //获取声道数量
            val frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE) //获取帧率
            val pcmEncoding = mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) //PCM编码 模拟信号编码
            Log.i("Test","采样率：${sampleRate}")
            Log.i("Test","码率：${bitRate}")
            Log.i("Test","声道数量：${channelCount}")
            Log.i("Test","帧率：${frameRate}")
            Log.i("Test","PCM编码：${pcmEncoding}")
        }catch (e:java.lang.Exception){

            Log.e("Test", "出错了", e)
        }
    }

    /**
     * 通过编码格式字符串获取指定轨道
     */
    private fun getTrackIndex(targetTrack: String): Int{
        var trackIndex = -1
        val count = mExtractor!!.trackCount //获取轨道数量
        for (i in 0 until count) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val currentTrack = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (currentTrack.startsWith(targetTrack)) {
                trackIndex = i
                break
            }
        }
        return trackIndex
    }

    override fun onDestroy() {
        mExtractor?.release()
        super.onDestroy()
    }
}
