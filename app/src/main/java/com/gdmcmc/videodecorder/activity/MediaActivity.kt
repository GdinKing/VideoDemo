package com.gdmcmc.videodecorder.activity

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import com.gdmcmc.videodecorder.R
import kotlinx.android.synthetic.main.activity_media.*
import java.lang.Exception

/**
 * MediaRecorder使用实例
 * @author king
 * @time 2020-3-25 9:31
 */
class MediaActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null

    //是否正在录制
    private var isRecording = false

    //文件保存位置
    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        filePath = "${externalCacheDir!!.absolutePath}/test.3gp"
    }

    /**
     * 开始录制
     */
    fun startRecord(view: View) {
        if (isRecording) {
            return
        }
        ct_time.start()
        mediaRecorder = MediaRecorder()
        //设置音频来源，这里是来自麦克风
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        //设置输出的音频文件格式,这里设置为3gp
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        //设置文件保存路径
        mediaRecorder?.setOutputFile(filePath)
        //设置音频编码，具体类型查看官网介绍
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        try {
            //准备
            mediaRecorder?.prepare()
        } catch (e: Exception) {
            Log.e("Test", "mediaRecorder.prepare() failed")
        }
        //开始录制
        mediaRecorder?.start()
        isRecording = true
    }

    /**
     * 停止录制
     */
    fun stopRecord(view: View) {
        ct_time.stop()
        ct_time.setBase(SystemClock.elapsedRealtime())
        mediaRecorder?.stop()
        //释放资源
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
    }

    /**
     * 播放音频
     */
    fun startPlay(view: View) {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            return
        }
        mediaPlayer = MediaPlayer()
        //设置音频数据来源
        mediaPlayer?.setDataSource(filePath)
        try {
            //缓冲
            mediaPlayer?.prepare()
        } catch (e: Exception) {
            Log.e("Test", "mediaPlayer.prepare() failed")
        }
        //播放
        mediaPlayer?.start()

        mediaPlayer?.setOnCompletionListener {
            //播放完毕监听
        }

        mediaPlayer?.setOnErrorListener { mp, what, extra ->
            //播放出错监听
            false
        }
    }

    /**
     * 停止播放
     */
    fun stopPlay(view: View) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
