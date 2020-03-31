package com.gdmcmc.videodecorder.activity

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gdmcmc.videodecorder.R
import java.io.File
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

    //缓冲区大小，缓冲区用于保存音频数据流
    private var bufferSize: Int = 0

    //记录是否正在录制音频
    @Volatile
    private var isRecording = false

    private var recordThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)
        initRecoder()
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
            AudioFormat.CHANNEL_IN_MONO,
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
            AudioFormat.CHANNEL_IN_MONO,
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
        val tmpFile: File? = createFile("${System.currentTimeMillis()}.pcm")
        //文件输出流
        var fos: FileOutputStream = FileOutputStream(tmpFile?.getAbsoluteFile())
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
                    fos.write(buffer)
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

    /**
     * 创建文件
     */
    private fun createFile(name: String): File? {
        val dirPath: String =
            Environment.getExternalStorageDirectory().absolutePath + "/AudioRecord/"
        val file = File(dirPath)
        if (!file.exists()) {
            file.mkdirs()
        }
        val filePath = dirPath + name
        val objFile = File(filePath)
        if (!objFile.exists()) {
            try {
                objFile.createNewFile()
                return objFile
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }
}
