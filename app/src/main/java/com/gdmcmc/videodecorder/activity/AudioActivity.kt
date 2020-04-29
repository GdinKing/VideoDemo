package com.gdmcmc.videodecorder.activity

import android.content.Context
import android.media.*
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gdmcmc.videodecorder.R
import kotlinx.android.synthetic.main.activity_audio.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


/**
 * AudioRecord录制音频
 *
 * @author king
 * @time 2020-3-26 09:49
 */
class AudioActivity : AppCompatActivity() {
    //音频录制
    private var audioRecord: AudioRecord? = null
    //音频播放
    private var audioTrack: AudioTrack? = null

    //缓冲区大小，缓冲区用于保存音频数据流
    private var bufferSize: Int = 0

    //AudioTrack缓冲区大小
    private var trackBufferSize: Int = 0

    //记录是否录制音频
    @Volatile
    private var isRecording = false

    //记录是否播放音频
    @Volatile
    private var isPlaying = false
    //录音线程
    private var recordThread: Thread? = null
    //播放线程
    private var playThread: Thread? = null

    private lateinit var mAudioManager:AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        initRecoder()

        initTrack()
    }

    /**
     * 初始化
     */
    private fun initRecoder() {
        /*
            getMinBufferSize用于获取成功创建AudioRecord对象所需的最小缓冲区大小,
            此大小不能保证在负载下能顺利录制，应根据预期的频率选择更高的值，
            在该频率下，将对AudioRecord实例进行轮询以获取新数据
            参数介绍：(具体看官网api介绍)
            sampleRateInHz：采样率，以赫兹为单位
            channelConfig：音频通道的配置
            audioFormat：音频数据的格式
         */
        bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        /*
            构建AudioRecord对象。
            参数介绍：
            audioSource：音频来源
            sampleRateInHz：采样率，以赫兹为单位。目前，只有44100Hz是保证在所有设备上都可以使用的速率，但是其他速率（例如22050、16000和11025）可能在某些设备上可以使用
            channelConfig：音频通道的配置
            audioFormat：音频数据的格式
            bufferSizeInBytes：在录制期间写入音频数据的缓冲区的总大小（以字节为单位）
        */
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )
    }

    /**
     * 开始录制
     */
    fun startRecord(view: View) {
        if (isRecording) {
            return
        }
        ct_time.base = System.currentTimeMillis()
        ct_time.start()
        isRecording = true
        if (recordThread == null) {
            recordThread = Thread(recordRunnable)
        }
        recordThread!!.start()
    }

    /**
     * 停止录制
     */
    fun stopRecord(view: View) {
        ct_time.stop()
        ct_time.base = System.currentTimeMillis()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null;
        recordThread = null
    }

    /**
     * 录音线程
     *
     * 由于需要不断读取音频数据，所以放在子线程操作
     */
    private val recordRunnable = Runnable {
        //设置线程优先级
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        //创建文件
        val tmpFile: File? = createFile("test.pcm")
        //文件输出流
        var fos: FileOutputStream = FileOutputStream(tmpFile?.absoluteFile)
        try {

            if (audioRecord?.getState() !== AudioRecord.STATE_INITIALIZED) {
                //没有初始化成功
                return@Runnable
            }
            //开始录制
            audioRecord?.startRecording()

            var buffer = 0
            val bytes = ByteArray(bufferSize)
            //轮询读取数据
            while (isRecording) {
                if (audioRecord != null) {
                    buffer = audioRecord!!.read(bytes, 0, bufferSize)
                    if (buffer == AudioRecord.ERROR_INVALID_OPERATION || buffer == AudioRecord.ERROR_BAD_VALUE) {
                        continue
                    }
                    if (buffer == 0 || buffer == -1) {
                        break
                    }
                    //在此可以对录制音频的数据进行二次处理 如变声，压缩，降噪等操作
                    //也可以直接发送至服务器（实时语音传输） 对方可采用AudioTrack进行播放
                    //这里直接将pcm音频数据写入文件
                    fos.write(bytes)
                }
            }
        } catch (e: Exception) {
            Log.e("Test", "出错了", e)
        } finally {
            try {
                fos?.close()
            } catch (ex: IOException) {
            }
        }
    }

    private fun initTrack() {
        //初始化一个缓冲区大小
        trackBufferSize = AudioTrack.getMinBufferSize(
            44100,//采样率
            AudioFormat.CHANNEL_OUT_STEREO,//音频通道的配置,指明声道数,与AudioRecord的对应
            AudioFormat.ENCODING_PCM_16BIT
        )//音频数据的格式
        val sessionId = mAudioManager.generateAudioSessionId();
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)//指明声音属于什么渠道，MUSIC-多媒体音乐
            .build()
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(44100)//采样率
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)//音频数据的格式
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)//音频通道,指明声道数
            .build();
        //创建audioTrack对象,api 23以后可以使用AudioTrack.Builder
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            trackBufferSize*2,//缓冲区大小
            AudioTrack.MODE_STREAM,//模式-流模式
            sessionId //用于区分的标识
        )
    }

    /**
     * 播放线程
     */
    private val playRunnable = Runnable {
        //设置线程优先级
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        //获取之前录音所获得的PCM文件
        val filePath = this.externalCacheDir!!.absolutePath + "/AudioRecord/test.pcm"
        val tmpFile: File? = File(filePath)
        //文件输入流
        var fis: FileInputStream = FileInputStream(tmpFile?.absoluteFile)
        try {
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                //没有初始化成功
                Log.e("Test","初始化失败")
                return@Runnable
            }
            Log.i("Test","开始播放")
            var buffer = ByteArray(trackBufferSize)
            //开始播放
            audioTrack?.play()
            //从文件流读取数据
            while (fis.read(buffer) !=-1) {

                if (!isPlaying) {
                    break
                }
                Log.i("Test","开始播放中")
                if (audioTrack != null) {
                    audioTrack!!.write(buffer, 0, buffer.size)
                }
            }
        } catch (e: Exception) {
            Log.e("Test", "出错了", e)
        } finally {
            try {
                fis?.close()
                //停止，释放资源
                audioTrack?.stop()
                audioTrack?.release()
            } catch (ex: IOException) {
            }
        }
    }

    /**
     * 开始播放
     */
    fun startPlay(view: View) {
        if (isPlaying) {
            return
        }
        isPlaying = true
        if (playThread == null) {
            playThread = Thread(playRunnable)
        }
        playThread!!.start()
    }

    /**
     * 停止播放
     */
    fun stopPlay(view: View) {
        isPlaying = false
        audioTrack = null
        playThread = null
    }

    /**
     * 创建文件
     */
    private fun createFile(name: String): File? {
        val dirPath: String = this.externalCacheDir!!.absolutePath + "/AudioRecord/"
        val file = File(dirPath)
        if (!file.exists()) {
            file.mkdirs()
        }
        val filePath = dirPath + name
        val objFile = File(filePath)
        if (objFile.exists()) {
            objFile.delete()
        }
        try {
            objFile.createNewFile()
            return objFile
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

}
