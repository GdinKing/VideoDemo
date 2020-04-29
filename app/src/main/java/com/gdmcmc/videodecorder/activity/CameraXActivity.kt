package com.gdmcmc.videodecorder.activity

import android.annotation.SuppressLint
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.TextureView
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.gdmcmc.videodecorder.R
import kotlinx.android.synthetic.main.activity_camera_x.*
import java.io.File
import java.lang.Math.abs
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

typealias LumaListener = (luma: Double) -> Unit

/**
 * CameraX使用示例
 *
 * @author king
 * @time 2020-4-1 9:23
 */
class CameraXActivity : AppCompatActivity() {

    companion object {
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private lateinit var cameraExecutor: ExecutorService

    private var preview: Preview? = null
    //拍照
    private var imageCapture: ImageCapture? = null

    private var camera: Camera? = null

    //摄像头位置，前置/后置
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private var isReversedHorizontal: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)
        pv_preview.post {
            initCamera()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun initCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        //PreviewView的实现形式，TextureView还是SurfaceView
        pv_preview.implementationMode = PreviewView.ImplementationMode.SURFACE_VIEW

        val metrics = DisplayMetrics().also { pv_preview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        val rotation = pv_preview.display.rotation
        //相机选择器，用于指定使用前置/后置摄像头
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
            //设置预览对象，用于展示预览界面
            preview?.setSurfaceProvider(pv_preview.previewSurfaceProvider)

            //创建拍照管理类对象
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(screenAspectRatio)//设置照片比例
                .setTargetRotation(rotation)//设置旋转角度
                .build()
            //图像分析
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)//设置照片比例
                .setTargetRotation(rotation)//设置旋转角度
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        // Values returned from our analyzer are passed to the attached listener
                        // We log image analysis results here - you should do something useful
                        // instead!
                        Log.d("Test", "Average luminosity: $luma")
                    })
                }
            //绑定到生命周期前先解绑所有之前绑定
            cameraProvider.unbindAll()

            //关联生命周期
            camera =
                CameraX.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 拍照
     */
    fun captureClick(view: View) {
        imageCapture?.let { imageCapture ->
            val file = createFile(
                getExternalFilesDir(Environment.DIRECTORY_DCIM)!!,
                System.currentTimeMillis().toString(),
                PHOTO_EXTENSION
            )
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // 图像文件输出配置
            val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
                .setMetadata(metadata)
                .build()
            //执行拍照
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(file)
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            applicationContext,
                            arrayOf(savedUri.toString()),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Log.i("Test", "Image capture scanned into media store: $uri")
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(applicationContext, "拍照出错", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    /**
     * 获取预览视图宽高比例
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.CHINESE)
                .format(System.currentTimeMillis()) + extension
        )

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * Our custom image analysis class.
     *
     * <p>All we need to do is override the function `analyze` with our desired operations. Here,
     * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
     */
    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Used to add listeners that will be called with each luma computed
         */
        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
         * call image.close() on received images when finished using them. Otherwise, new images
         * may not be received or the camera may stall, depending on back pressure setting.
         *
         */
        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }
    }


}
