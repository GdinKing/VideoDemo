package com.gdmcmc.videodecorder.util

import android.app.Activity
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.media.CamcorderProfile
import android.util.Log
import android.view.Surface
import java.util.*
import kotlin.math.abs

/**
 *
 *
 * @author king
 * @date 2020-03-31 08:59
 */
object CameraUtil {

    //允许最大的视频宽高
    const val MAX_VIDEO_SIZE = 3000

    /**
     * 获取适合的视频宽高
     * @param bl 高宽比，一般传入屏幕高宽比，或者UI上视频区域的高宽比
     */
    fun findFitVideoSize(cameraParameters: Camera.Parameters, bl: Float): Camera.Size {
        val supportedVideoSizes= cameraParameters.supportedVideoSizes
        var resultSize: Camera.Size? = null
        for (size in supportedVideoSizes) {
            Log.i("Test","${size.width}===${size.height}")
            if (size.width.toFloat() / size.height == bl && (size.width <= MAX_VIDEO_SIZE || size.height <= MAX_VIDEO_SIZE)) {
                if (resultSize == null) {
                    resultSize = size
                } else if (size.width > resultSize.width) {
                    resultSize = size
                }
            }
        }
        return resultSize ?: supportedVideoSizes[0]
    }

    /**
     * 相机画面缩放
     * @param params 相机参数
     * @param scaleFactor 缩放比例
     */
    fun setZoom(params: Camera.Parameters, scaleFactor: Float): Camera.Parameters? {
        if (!params.isZoomSupported) return params
        try {
            val addValue = ((scaleFactor - 1.0f) * params.maxZoom).toInt()
            var value = params.zoom + addValue
            if (value > params.maxZoom) {
                value = params.maxZoom
            }
            if (value < 0) {
                value = 0
            }
            params.zoom = value
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    /**
     * 获取适合的视频码率
     * 影响视频清晰度和文件大小
     * @param cameraID 摄像头ID
     */
    fun getBestCamcorderProfile(cameraID: Int): CamcorderProfile? {
        var profile: CamcorderProfile? = null
        //视频码率
        when {

            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_2160P) -> { //2160P
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_2160P)
            }
            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_1080P) -> { //1080P
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_1080P)
            }
            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_720P) -> { //720P
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_720P)
            }
            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_480P) -> { //480P
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_480P)
            }
            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_HIGH) -> {//高品质
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_HIGH)
                return profile
            }
            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_CIF) -> {
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_CIF)
                return profile
            }
            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_QVGA) -> {
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_QVGA)
            }
            CamcorderProfile.hasProfile(cameraID, CamcorderProfile.QUALITY_LOW) -> {
                profile = CamcorderProfile.get(cameraID, CamcorderProfile.QUALITY_LOW)
            }
        }
//        if (profile != null) {
//            //视频码率
//            profile.videoBitRate = 6000000
//        }
        return profile
    }

    /**
     * 检查是否有前置摄像头
     *
     * @return
     */
    fun hasFrontCamera(): Boolean {
        val cameraCount = Camera.getNumberOfCameras()
        val info = CameraInfo()
        for (i in 0 until cameraCount) {
            Camera.getCameraInfo(i, info)
            if (CameraInfo.CAMERA_FACING_FRONT == info.facing) return true
        }
        return false
    }

    /**
     * 针对安卓6.0以下，判断是否有相机权限
     */
    fun isCameraCanUse(): Boolean {
        var isCanUse = true
        var mCamera: Camera? = null
        try {
            mCamera = Camera.open()
            val mParameters = mCamera.parameters //针对魅族手机
            mCamera.parameters = mParameters
        } catch (e: java.lang.Exception) {
            isCanUse = false
        }
        if (mCamera != null) {
            try {
                mCamera.release()
            } catch (e: Exception) {
                return isCanUse
            }
        }
        return isCanUse
    }

    /**
     * 获取录像视频角度
     *
     * @param cameraPosition 相机位置(前置/后置)
     * @param rotation       当前旋转角度
     * @return
     */
    fun getRecorderOrientation(cameraPosition: Int, rotation: Int): Int {
        return if (cameraPosition == CameraInfo.CAMERA_FACING_FRONT && rotation == 0) {
            270
        } else abs(rotation - 90)
    }

    /**
     * 获取正确的预览角度
     * setDisplayOrientation本身只能改变预览的角度
     * @param cameraID 相机ID
     */
    fun getCameraPreviewOrientation(cameraID: Int, activity: Activity): Int {
        val info = Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, info);
        //屏幕选择角度
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 ->
                degrees = 0
            Surface.ROTATION_90 ->
                degrees = 90
            Surface.ROTATION_180 ->
                degrees = 180
            Surface.ROTATION_270 ->
                degrees = 270
        }
        var result = 90
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }
        return result;
    }


    /**
     * 设置自动对焦模式
     * @return
     */
    fun setAutoFocusMode(params: Camera.Parameters?) {
        val modes: MutableList<String> = params?.getSupportedFocusModes() ?: return
        if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            //连续自动对焦,这是录制视频时对焦模式的最好选择
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            //自动对焦模式，只对焦一次
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO)
        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            //固定焦点模式
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED)
        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
            //无穷对焦模式
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY)
        } else {
            params.setFocusMode(modes[0])
        }
    }

    /**
     * 找出最合适的预览尺寸，规则如下：
     * 1.将尺寸按比例分组，找出比例最接近屏幕比例的尺寸组
     * 2.在比例最接近的尺寸组中找出最接近屏幕尺寸且大于屏幕尺寸的尺寸
     * 3.如果没有找到，则忽略2中第二个条件再找一遍，应该是最合适的尺寸了
     *
     * 这里需要注意横竖屏传入的值不同
     * @param surfaceWidth 预览区域的宽
     * @param surfaceHeight 预览区域的高
     */
    fun findFitPreviewSize(surfaceWidth: Int,surfaceHeight: Int, sizeList: List<Camera.Size>?): Camera.Size? {
        if (surfaceWidth <= 0 || surfaceHeight <= 0 || sizeList == null) {
            return null
        }
        val ratioListList: MutableList<MutableList<Camera.Size>> =
            ArrayList()
        for (size in sizeList) {
            addRatioList(ratioListList, size)
        }
        val surfaceRatio = surfaceWidth.toFloat() / surfaceHeight
        var bestRatioList: List<Camera.Size>? = null
        var ratioDiff = Float.MAX_VALUE
        for (ratioList in ratioListList) {
            val ratio =
                ratioList[0].width.toFloat() / ratioList[0].height
            val newRatioDiff = Math.abs(ratio - surfaceRatio)
            if (newRatioDiff < ratioDiff) {
                bestRatioList = ratioList
                ratioDiff = newRatioDiff
            }
        }
        var bestSize: Camera.Size? = null
        var diff = Int.MAX_VALUE
        assert(bestRatioList != null)
        for (size in bestRatioList!!) {
            val newDiff =
                Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight)
            if (size.height >= surfaceHeight && newDiff < diff) {
                bestSize = size
                diff = newDiff
            }
        }
        if (bestSize != null) {
            return bestSize
        }
        diff = Int.MAX_VALUE
        for (size in bestRatioList) {
            val newDiff =
                Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight)
            if (newDiff < diff) {
                bestSize = size
                diff = newDiff
            }
        }
        return bestSize
    }

    private fun addRatioList(ratioListList: MutableList<MutableList<Camera.Size>>, size: Camera.Size) {
        val ratio = size.width.toFloat() / size.height
        for (ratioList in ratioListList) {
            val mine =
                ratioList[0].width.toFloat() / ratioList[0].height
            if (ratio == mine) {
                ratioList.add(size)
                return
            }
        }
        val ratioList: MutableList<Camera.Size> =
            ArrayList()
        ratioList.add(size)
        ratioListList.add(ratioList)
    }
}