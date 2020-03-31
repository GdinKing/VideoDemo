package com.gdmcmc.videodecorder.activity

import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.gdmcmc.videodecorder.R
import com.gdmcmc.videodecorder.util.CameraUtil
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_camera.view.*
import java.io.File


/**
 *  Camera使用实例
 * 建议将此Activity固定方向，然后使用传感器监听横竖屏切换，否则预览容易出问题
 * @author king
 * @time 2020-3-30 14:22
 */
class CameraActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var camera: Camera? = null
    private var recorder: MediaRecorder? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)//取消标题栏
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )//全屏
        setContentView(R.layout.activity_camera)
        sv_camera.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        sv_camera.holder.setKeepScreenOn(true)
        sv_camera.holder.addCallback(this)
    }

    /**
     * 初始化相机设置
     */
    private fun startPreview() {
        if (camera != null) {
            stopPreview()
        }
        try {
            //开启后置摄像头
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
            //设置预览角度
            val rotation =
                CameraUtil.getCameraPreviewOrientation(Camera.CameraInfo.CAMERA_FACING_BACK, this)
            camera?.setDisplayOrientation(rotation)

            // 在SurfaceView上预览
            camera?.setPreviewDisplay(sv_camera.holder)
            val parameters = camera?.getParameters()
            //设置自动对焦模式
            CameraUtil.setAutoFocusMode(parameters)
            //获取支持的预览大小，注意这里获取的宽高是根据横屏的
            val sizes = parameters?.supportedPreviewSizes
            //宽高建议根据横竖屏切换
            val previewSize =
                CameraUtil.findFitPreviewSize(sv_camera.height, sv_camera.width, sizes)
            if (previewSize != null) {
                //设置预览大小
                parameters?.setPreviewSize(previewSize.width, previewSize.height)
            }
            camera?.setParameters(parameters)
            //自动对焦
//            camera?.autoFocus { success, camera -> }
            //预览回调，可以在这里获取NV21格式的数据或YUV数据
//            camera?.setPreviewCallback { data, camera ->  }
            //开始预览
            camera?.startPreview()
            camera?.lock()
        } catch (e: Exception) {
            Log.e("Test", "出错了", e)
        }
    }

    /**
     * 停止预览
     */
    private fun stopPreview() {
        camera?.stopPreview()
        camera?.setPreviewCallback(null)
        camera?.release()
        camera = null
    }

    /**
     * 开始录制
     * 注意方法调用的先后顺序
     */
    private fun startRecorder() {
        if (camera == null) {
            return
        }
        if (recorder != null) {
            stopRecord()
        }
        try {
            val videoSize = CameraUtil.findFitVideoSize(
                camera!!.parameters,
                sv_camera.height / sv_camera.width.toFloat()
            )
            //先停止camera预览,释放camera
            camera?.stopPreview()
            camera?.unlock()
            //创建MediaRecorder对象
            recorder = MediaRecorder()
            //关联camera
            recorder?.setCamera(camera)
            //设置视频角度；
            val rotation =
                CameraUtil.getCameraPreviewOrientation(Camera.CameraInfo.CAMERA_FACING_BACK, this)
            recorder?.setOrientationHint(rotation)
            //设置预览区域
            recorder?.setPreviewDisplay(sv_camera.holder.surface)
            //设置音频来源
            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            //设置视频来源，来自摄像头
            recorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
            //音频编码方式
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            //设置视频编码
            recorder?.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP)
            //设置输出格式
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            //设置视频码率
            recorder?.setVideoEncodingBitRate(6 * 1000000)
            //设置视频帧率，注意设备支持,设置过高可能报错
            recorder?.setVideoFrameRate(30)
            //设置视频宽高,因为设置了上面那些参数，所以这里最高只能设置640*480，如果需要其他值，请参考startRecorder2
            recorder?.setVideoSize(640, 480)
            val file =
                File(getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath, "test.mp4")
            //设置音频文件的存储位置 {
            recorder?.setOutputFile(file.absolutePath)
            //准备
            recorder?.prepare()
            //开始录制
            recorder?.start()
            ct_time.start()
        } catch (e: Exception) {
            Log.e("Test", e.message, e)
        }

    }

    /**
     * 如果使用了setProfile，则使用此方法
     */
    private fun startRecorder2() {
        if (camera == null) {
            return
        }
        if (recorder != null) {
            stopRecord()
        }
        try {
            val videoSize = CameraUtil.findFitVideoSize(camera!!.parameters,
                sv_camera.height / sv_camera.width.toFloat()
            )
            //先停止camera预览,释放camera
            camera?.stopPreview()
            camera?.unlock()
            //创建MediaRecorder对象
            recorder = MediaRecorder()
            //关联camera
            recorder?.setCamera(camera)
            //设置视频角度；
            val rotation =
                CameraUtil.getCameraPreviewOrientation(Camera.CameraInfo.CAMERA_FACING_BACK, this)
            recorder?.setOrientationHint(rotation)
            //设置预览区域
            recorder?.setPreviewDisplay(sv_camera.holder.surface)
            //设置音频来源
            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            //设置视频来源，来自摄像头
            recorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)

            //设置输出格式
//            recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            val profile = CameraUtil.getBestCamcorderProfile(Camera.CameraInfo.CAMERA_FACING_BACK)
            if (profile != null) {
                //设置视频码率
                recorder?.setProfile(profile)
            }
            //设置视频宽高
            recorder?.setVideoSize(videoSize.width, videoSize.height)
            val file =
                File(getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath, "test.mp4")
            //设置音频文件的存储位置 {
            recorder?.setOutputFile(file.absolutePath)
            //准备
            recorder?.prepare()
            //开始录制
            recorder?.start()
            ct_time.start()
        } catch (e: Exception) {
            Log.e("Test", e.message, e)
        }

    }

    /**
     * 停止录制
     */
    private fun stopRecord() {
        ct_time.stop()
        recorder?.stop()
        recorder?.reset()
        recorder?.release()
        recorder = null
    }

    fun start(view: View) {
        startRecorder2()
    }

    fun stop(view: View) {
        stopRecord()
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecord()
        sv_camera.holder.removeCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        stopPreview()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        startPreview()
    }
}
