package com.gdmcmc.videodecorder.activity

import android.content.pm.ActivityInfo
import android.content.res.AssetFileDescriptor
import android.media.*
import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioTrack.MODE_STREAM
import android.media.MediaCodec.BufferInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.gdmcmc.videodecorder.R
import kotlinx.android.synthetic.main.activity_decoder.*
import java.nio.ByteBuffer
import kotlin.math.max


/**
 * MediaCodec解码播放视频
 * @author king
 * @time 2020-5-11 11:46
 */
class DecoderActivity : AppCompatActivity(), SurfaceHolder.Callback {
    //视频路径
    private var videoPath: String? = null
    @Volatile
    private var isDecoding = false
    //媒体提取器
    private var videoExtractor: MediaExtractor? = null
    private var audioExtractor: MediaExtractor? = null
    //视频解码器
    private var videoCodec: MediaCodec? = null
    //音频解码器
    private var audioCodec: MediaCodec? = null

    private var decodeThread: Thread? = null

    private var decodeAudioThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decoder)

        videoPath = intent.getStringExtra("videoPath")

        sv_video.holder.setKeepScreenOn(true)
        sv_video.holder.addCallback(this)

    }

    private fun init() {
        try {
            if (videoPath.isNullOrBlank()) {
                return
            }
            //创建MediaExtractor对象
            videoExtractor = MediaExtractor()
            audioExtractor = MediaExtractor()
            val uri = Uri.parse(videoPath)
            val fd: AssetFileDescriptor? = contentResolver.openAssetFileDescriptor(uri, "r");
            //设置视频数据源，可以是本地文件也可以是网络文件，这里我使用了手机本机拍摄的示例文件
            videoExtractor?.setDataSource(fd!!.fileDescriptor)
            audioExtractor?.setDataSource(fd!!.fileDescriptor)

            val count = videoExtractor!!.trackCount //获取轨道数量
            //视频
            for (i in 0 until count) {
                val mediaFormat = videoExtractor!!.getTrackFormat(i)
                val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mimeType.startsWith("video/")) {
                    videoExtractor?.selectTrack(i)
                    initVideo(mediaFormat)
                    break
                }
            }
            //音频
            for (i in 0 until audioExtractor!!.trackCount) {
                val mediaFormat = audioExtractor!!.getTrackFormat(i)
                val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mimeType.startsWith("audio/")) {
                    audioExtractor?.selectTrack(i)
                    initAudio(mediaFormat)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("Test", "出错了", e)
        }
    }

    private fun initAudio(mediaFormat: MediaFormat) {
        val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
        //获取手机支持的codecInfo，如果不支持，则无法解码该类型的视频
        val codecInfo = getCodecInfo(mimeType)

        sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)//音频采样率
        Log.i("Test", "采样率:" + sampleRate)
        if (codecInfo != null) {
            audioCodec = MediaCodec.createDecoderByType(mimeType)
            //crypto:数据加密 flags:解码器/编码器
            audioCodec?.configure(mediaFormat, null, null, 0)
            audioCodec?.start()

        } else {
            Log.e("Test", "格式错误")
        }
    }

    private var sampleRate: Int = 0

    private fun initVideo(mediaFormat: MediaFormat) {
        val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
        val codecInfo = getCodecInfo(mimeType)

        if (codecInfo != null) {

            changeVideoSize(mediaFormat)
            videoCodec = MediaCodec.createDecoderByType(mimeType)
            //crypto:数据加密 flags:解码器/编码器
            videoCodec?.configure(mediaFormat, sv_video.holder.surface, null, 0)
            videoCodec?.start()

        } else {
            Log.e("Test", "格式错误")
        }
    }

    private val decodeAudioRunnable = Runnable {
        if (audioCodec == null) {
            return@Runnable
        }

        try {
            val inputBuffers: Array<ByteBuffer> = audioCodec!!.getInputBuffers()
            var outputBuffers: Array<ByteBuffer> = audioCodec!!.getOutputBuffers()
            val info = BufferInfo()
            val buffsize = AudioTrack.getMinBufferSize(
                sampleRate,
                CHANNEL_OUT_STEREO,
                ENCODING_PCM_16BIT
            )

            // 创建AudioTrack对象
            var audioTrack: AudioTrack? = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                CHANNEL_OUT_STEREO,
                ENCODING_PCM_16BIT,
                buffsize,
                MODE_STREAM
            )
            //启动AudioTrack
            audioTrack!!.play()
            while (isDecoding) {
                val inIndex: Int = audioCodec!!.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = inputBuffers[inIndex]
                    //从MediaExtractor中读取一帧待解数据
                    val sampleSize = audioExtractor!!.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        audioCodec!!.queueInputBuffer(
                            inIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    } else { //向MediaDecoder输入一帧待解码数据
                        audioCodec!!.queueInputBuffer(
                            inIndex,
                            0,
                            sampleSize,
                            videoExtractor!!.sampleTime,
                            0
                        )
                        audioExtractor!!.advance()
                    }
                    //从MediaDecoder队列取出一帧解码后的数据
                    val outIndex: Int = audioCodec!!.dequeueOutputBuffer(info, 10000)
                    when (outIndex) {
                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            outputBuffers = audioCodec!!.getOutputBuffers()
                        }
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format: MediaFormat = audioCodec!!.getOutputFormat()
                            audioTrack.playbackRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d(
                            "Test",
                            "dequeueOutputBuffer timed out!"
                        )
                        else -> {
                            val outBuffer = outputBuffers[outIndex]
                            //Log.v(TAG, "outBuffer: " + outBuffer);
                            val chunk = ByteArray(info.size)
                            // Read the buffer all at once
                            outBuffer[chunk]
                            //清空buffer,否则下一次得到的还会得到同样的buffer
                            outBuffer.clear()
                            // AudioTrack write data
                            audioTrack.write(chunk, info.offset, info.offset + info.size)
                            audioCodec!!.releaseOutputBuffer(outIndex, false)
                        }
                    }
                    // 所有帧都解码、播放完之后退出循环
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
            //释放MediaDecoder资源
            audioCodec?.stop()
            audioCodec?.release()
            audioCodec = null
            audioExtractor?.release()
            audioExtractor = null
            //释放AudioTrack资源
            audioTrack.stop()
            audioTrack.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e("Test", "", e)
        }
    }

    private val decodeVideoRunnable = Runnable {
        try {
            //存放目标文件的数据
            var byteBuffer: ByteBuffer? = null
            //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
            val info = MediaCodec.BufferInfo()
            videoCodec!!.getOutputBuffers()
            var first = false
            var startWhen: Long = 0
            var isInput = true
            while (isDecoding) {
//            Thread.sleep(30)//可以控制慢放
                if (isInput) {
                    //1 准备填充器
                    val inIndex = videoCodec!!.dequeueInputBuffer(10000)

                    if (inIndex >= 0) {
                        //2 准备填充数据

                        byteBuffer = videoCodec!!.getInputBuffer(inIndex)

                        val sampleSize = videoExtractor!!.readSampleData(byteBuffer!!, 0)

                        if (videoExtractor!!.advance() && sampleSize > 0) {
                            videoCodec!!.queueInputBuffer(
                                inIndex,
                                0,
                                sampleSize,
                                videoExtractor!!.sampleTime,
                                0
                            )

                        } else {
                            videoCodec!!.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInput = false
                        }
                    } else {
                        continue
                    }
                }
                //4 开始解码
                val outIndex = videoCodec!!.dequeueOutputBuffer(info, 10000)

                if (outIndex >= 0) {
                    when (outIndex) {
                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            Log.d("Test", "INFO_OUTPUT_BUFFERS_CHANGED")
                            videoCodec!!.getOutputBuffers()
                        }
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                            Log.d(
                                "Test",
                                "INFO_OUTPUT_FORMAT_CHANGED format : " + videoCodec!!.getOutputFormat()
                            )
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        }
                        else -> {
                            if (!first) {
                                startWhen = System.currentTimeMillis();
                                first = true;
                            }
                            try {
                                val sleepTime: Long =
                                    info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen)
                                //Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);
                                if (sleepTime > 0) Thread.sleep(sleepTime)
                            } catch (e: InterruptedException) { // TODO Auto-generated catch block
                                e.printStackTrace()
                            }
                            //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
                            //调用这个api之后，SurfaceView才有图像
                            videoCodec!!.releaseOutputBuffer(outIndex, true /* Surface init */)
                        }
                    }
                }
            }
            videoCodec?.stop()
            videoCodec?.release()
            videoExtractor?.release()
        } catch (e: Exception) {
            Log.e("Test", "", e)
        }
    }

    /**
     * 根据视频大小改变SurfaceView大小
     */
    private fun changeVideoSize(mediaFormat: MediaFormat) {
        var videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH) //获取高度
        var videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT) //获取高度
        val surfaceWidth = sv_video.measuredWidth
        val surfaceHeight = sv_video.measuredHeight
        //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
        var maxSize: Double
        maxSize =
            if (resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                //竖屏模式下按视频宽度计算放大倍数值
                max(videoWidth / surfaceWidth.toDouble(), videoHeight / surfaceHeight.toDouble())
            } else {
                //横屏模式下按视频高度计算放大倍数值
                max(videoWidth / surfaceHeight.toDouble(), videoHeight / surfaceWidth.toDouble())
            }

        //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
        videoWidth = Math.ceil(videoWidth / maxSize).toInt();
        videoHeight = Math.ceil(videoHeight / maxSize).toInt();

        //将计算出的视频尺寸设置到surfaceView 让视频自动填充。
        sv_video.layoutParams = ConstraintLayout.LayoutParams(videoWidth, videoHeight);
    }

    /**
     * 获取可用的MediaCodecInfo
     */
    private fun getCodecInfo(mimeType: String): MediaCodecInfo? {
        val codeInfos = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        for (info in codeInfos) {
            if (info.isEncoder) {
                continue
            }
            val supportMimes = info.supportedTypes
            for (i in supportMimes.indices) {
                val type = supportMimes[i]
                if (type.equals(mimeType, true)) {
                    return info
                }
            }

        }
        return null
    }

    fun playClick(view: View) {
        if (!isDecoding) {
            if (decodeThread == null) {
                decodeThread = Thread(decodeVideoRunnable)
            }
            decodeThread!!.start()
            if (decodeAudioThread == null) {
                decodeAudioThread = Thread(decodeAudioRunnable)
            }
            decodeAudioThread!!.start()
            isDecoding = true
        } else {
            isDecoding = false
        }
    }

    /**
     * 释放资源
     */
    private fun release() {
        isDecoding = false
        decodeThread = null
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {

        init()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        release()
    }

}
