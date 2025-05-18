package com.example.myglsurfaceviewapp


import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.PI

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val cntx: Context = context
    private val renderer: MyGLRenderer
    private var previousX = 0f
    private var previousY = 0f
    
    init
    {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(cntx)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    
    private var lastX = 0f
    private var lastY = 0f
    
    private var initialSpacing = 0f
    private var currentSpacing = 0f
    
    private var initialAngle = 0f
    private var currentAngle = 0f
    
    private var isZooming = false
    private var isPanning = false
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                isPanning = true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    initialSpacing = getSpacing(event)
                    if (initialSpacing > 10f) {
                        isZooming = true
                        isPanning = false // ズーム中はパンを無効にする（必要に応じて）
                        initialAngle = getAngle(event)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPanning && event.pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    // ここで dx, dy を使って上下左右の移動処理を行う
                    // 例：v.translationX += dx
                    //     v.translationY += dy
//                    renderer.angleX+=dy*0.5f
                    renderer.angleY+=dx*0.5f
                    renderer.att = 1000f
                    println("パン: dx=$dx, dy=$dy")
                    lastX = event.x
                    lastY = event.y
                } else if (isZooming && event.pointerCount == 2) {
                    currentSpacing = getSpacing(event)
                    if (currentSpacing > 10f) {
                        val zoomFactor = currentSpacing / initialSpacing
                        // ここで zoomFactor を使ってズーム処理を行う
                        // 例：v.scaleX *= zoomFactor
                        //     v.scaleY *= zoomFactor
                        println("ズーム: factor=$zoomFactor")
                        initialSpacing = currentSpacing
                        
                        currentAngle = getAngle(event)
                        val rotation = currentAngle - initialAngle
                        // ここで rotation を使って回転処理を行う（必要に応じて）
                        // 例：v.rotation += rotation
                        println("回転: angle=$rotation")
                        initialAngle = currentAngle
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                isPanning = false
                isZooming = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (isZooming && event.pointerCount < 2) {
                    isZooming = false
                    isPanning = true // 指が一本になったらパンを有効にする（必要に応じて）
                    lastX = event.getX(0)
                    lastY = event.getY(0)
                }
            }
        }
        return true // イベントを消費したことを示す
    }
    
    private fun getSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return hypot(x, y)
    }
    
    private fun getAngle(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        return atan2(deltaY, deltaX).toFloat() * 180f / PI.toFloat()
    }
   
}
