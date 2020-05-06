package com.gdmcmc.videodecorder

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import com.gdmcmc.videodecorder.activity.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        //申请权限
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ), 100
        )
    }

    private fun initView() {
//        val surfaceView = ImageSurfaceView(this)
//        val lp = ViewGroup.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.MATCH_PARENT
//        )
//        rl_container.addView(surfaceView, lp)
    }

    fun toSurface(view: View) {
        val intent = Intent(this, SurfaceActivity::class.java)
        startActivity(intent)
    }

    fun toTexture(view: View) {
        val intent = Intent(this, TextureActivity::class.java)
        startActivity(intent)
    }

    fun toMedia(view: View) {
        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }

    fun toAudio(view: View) {
        val intent = Intent(this, AudioActivity::class.java)
        startActivity(intent)
    }

    fun toCamera(view: View) {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    fun toCameraX(view: View) {
        val intent = Intent(this, CameraXActivity::class.java)
        startActivity(intent)
    }

    fun toMediaExtractor(view: View) {

        val intent = Intent(this, MediaExtractorActivity::class.java)
        startActivity(intent)
    }
}
