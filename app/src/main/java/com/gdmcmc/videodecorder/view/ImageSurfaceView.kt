package com.gdmcmc.videodecorder.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.Exception

/**
 *
 *
 * @author king
 * @date 2020-03-09 15:57
 */
class ImageSurfaceView(context: Context?) : SurfaceView(context), SurfaceHolder.Callback {

    var surfaceHolder: SurfaceHolder? = null
    var drawThread: DrawThread? = null

    init {
        surfaceHolder = holder
        surfaceHolder?.addCallback(this)
        if (surfaceHolder != null) {
            drawThread = DrawThread(surfaceHolder!!)
        }
    }


    override fun surfaceCreated(holder: SurfaceHolder?) {
        //surface创建的时候调用，一般在该方法中启动绘图的线程
        Log.i("Test", "surface创建")
        drawThread?.isRunning = true
        drawThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        //surface尺寸发生改变的时候调用，如横竖屏切换,或者初次渲染在屏幕上时
        Log.i("Test", "surface更新")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        //surface被销毁的时候调用，一般在该方法中停止绘图线程
        Log.i("Test", "surface销毁")
        drawThread?.isRunning = false
    }


    /**
     * 异步线程，不停地绘制文字
     */
    inner class DrawThread(val sHolder: SurfaceHolder) : Thread() {
        var isRunning = true
        var count = 0

        override fun run() {
            while (isRunning) {
                var c: Canvas? = null
                try {
                    synchronized(sHolder) {
                        c = sHolder.lockCanvas()//获取画布
                        c?.drawColor(Color.WHITE)//画布背景白色
                        val paint = Paint()
                        paint.color = Color.RED
                        paint.textSize = 60f

                        c?.drawText("${count++}秒", 100f, 310f, paint)//绘制时间
                        sleep(1000)
                    }
                }catch (e:Exception){

                }finally {
                    if (c != null) {
                        sHolder.unlockCanvasAndPost(c)//释放画布锁，方便下一次绘制
                    }
                }

            }

        }
    }
}