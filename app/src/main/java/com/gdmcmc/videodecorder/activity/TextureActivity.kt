package com.gdmcmc.videodecorder.activity

import android.graphics.SurfaceTexture
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import com.gdmcmc.videodecorder.R
import kotlinx.android.synthetic.main.activity_texture.*
import java.lang.Exception
/**
 * TextureView使用示例
 *
 * @author king
 * @time 2020-3-17 14:48
 */
class TextureActivity : AppCompatActivity(),TextureView.SurfaceTextureListener {

    private var camera : Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_texture)
        tv_video.surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        //在SurfaceTexture准备使用时调用
        camera = Camera.open()
        try{
            camera?.setPreviewTexture(tv_video.surfaceTexture)
            camera?.setDisplayOrientation(90)
            camera?.startPreview()
        }catch (e:Exception){

        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        //当SurfaceTexture缓冲区大小更改时调用
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        //当SurfaceTexture有更新时调用
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        //当指定SurfaceTexture即将被销毁时调用。
        //如果返回true，则调用此方法后，表面纹理中不会发生渲染。
        //如果返回false，则客户端需要调用release()。大多数应用程序应该返回true
        camera?.stopPreview()
        camera?.release()
        return true
    }

}
