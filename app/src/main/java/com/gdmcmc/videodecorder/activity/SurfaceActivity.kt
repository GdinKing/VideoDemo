package com.gdmcmc.videodecorder.activity

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import com.gdmcmc.videodecorder.R
import kotlinx.android.synthetic.main.activity_surface.*
import java.io.File
import java.lang.Exception
/**
 * SurfaceView使用示例
 * @author king
 * @time 2020-3-17 14:48
 */
class SurfaceActivity : AppCompatActivity(), SurfaceHolder.Callback {
    //媒体播放控制器
    private var mediaPlayer: MediaPlayer? = null
    //视频路径
    private var videoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surface)

        ////把输送给surfaceView的视频画面，直接显示到屏幕上,不要维持它自身的缓冲区
        sv_video.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        sv_video.holder.setKeepScreenOn(true)
        sv_video.holder.addCallback(this)
        initMedia()
        btn_play.setOnClickListener {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                pause()
            } else {
                start()
            }
        }
    }

    /**
     * 初始化媒体播放
     */
    private fun initMedia() {
        videoPath = "/storage/emulated/0/DCIM/Camera/VID_20200311_082801.mp4"

        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(videoPath)
        mediaPlayer?.setOnPreparedListener {
            start()//缓冲完，播放
        }
        mediaPlayer?.setOnCompletionListener {
            Toast.makeText(this,"播放完毕",Toast.LENGTH_SHORT).show()
            btn_play.text = "重新播放"
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mediaPlayer?.setDisplay(holder)//设置播放的容器
        mediaPlayer?.prepareAsync()
    }

    private fun start(){
        btn_play.text = "暂停"
        mediaPlayer?.start()
    }

    private fun pause(){
        btn_play.text = "播放"
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
