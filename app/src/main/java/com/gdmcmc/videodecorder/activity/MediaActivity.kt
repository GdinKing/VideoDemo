package com.gdmcmc.videodecorder.activity

import android.media.MediaMuxer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gdmcmc.videodecorder.R

/**
 * MediaExtractor 和 MediaMuxer使用示例
 * @author king
 * @time 2020-4-29 8:56
 */
class MediaActivity : AppCompatActivity() {

    private var mediaMuxer:MediaMuxer?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media2)
        init()
    }

    private fun init(){
        mediaMuxer = MediaMuxer("",MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    }
}
