package com.gdmcmc.videodecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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

    fun toMediaCodec(view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*" //选择视频文件
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, 1)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == 1) {
            val uri: Uri? = data?.data
            val docUrl = DocumentsContract.getDocumentId(uri)
            val split: List<String> = docUrl.split(":")
            val type = split[0]
            val id = split[1]
            val path = getVideoPath(id)
            val intent = Intent(this, DecoderActivity::class.java)
            intent.putExtra("videoPath", path)
            startActivity(intent)
        }


    }

    fun getVideoPath(id: String): String? {
        val VIDEO_PROJECTION = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID
        )
        //兼容androidQ
        val queryPathKey = MediaStore.Video.Media._ID
        val selection = "$queryPathKey=? "
        val videoCursor: Cursor? = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            VIDEO_PROJECTION, selection,
            arrayOf(id), null
        )
        var path: String? = ""
        if (videoCursor != null && videoCursor.moveToFirst()) {
            path = videoCursor.getString(videoCursor.getColumnIndexOrThrow(VIDEO_PROJECTION[0]))
            val id: Int? =
                videoCursor.getInt(videoCursor.getColumnIndexOrThrow(VIDEO_PROJECTION[1]))
//Android Q 公有目录只能通过Content Uri + id的方式访问，以前的File路径全部无效，如果是Video，记得换成MediaStore.Videos
            //Android Q 公有目录只能通过Content Uri + id的方式访问，以前的File路径全部无效，如果是Video，记得换成MediaStore.Videos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                path = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    .buildUpon()
                    .appendPath(id.toString()).build().toString()

            }
        }
        videoCursor?.close()
        return path
    }
}
