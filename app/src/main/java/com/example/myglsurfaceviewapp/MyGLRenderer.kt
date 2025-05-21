package com.example.myglsurfaceviewapp

import android.util.Log
import android.content.Context
import android.content.res.AssetManager
import java.io.InputStream

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer(context: Context) : GLSurfaceView.Renderer
{
    private val cntx: Context = context
    private lateinit var earth: World98
    private lateinit var path: PathRenderer
    private lateinit var immediate: ImmediateDraw

    private val mVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mModelMatrix = FloatArray(16)

    var angleX = 0f
    var angleY = 0f
    var att=0f

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?)
    {
        GLES20.glClearColor(0f, 0f, 0.64f, 1f)
        earth = World98(cntx)
        path = PathRenderer(30000)
        immediate = ImmediateDraw()
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int)
    {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
    //    Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 5000.0f)
        Matrix.perspectiveM(mProjectionMatrix, 0, 45.0f, ratio, 1.0f, 5000.0f)
        
    }

    override fun onDrawFrame(unused: GL10?)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        //Log.v("MyGLRenderer", "x=${angleX} y=${angleY}")
        Matrix.setLookAtM(mViewMatrix, 0,
            0f, 0f, 500f,
            0f, 0f, 0f,
            0f, 1.0f, 0.0f)

        path.setViewMatrix(mViewMatrix);
        immediate.setViewMatrix(mViewMatrix);
        
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.rotateM(mModelMatrix, 0, angleX, 1f, 0f, 0f)
        Matrix.rotateM(mModelMatrix, 0, angleY, 0f, 1f, 0f)
        path.setWorldMatrix(mModelMatrix);
        immediate.setWorldMatrix(mModelMatrix);
        path.setProjectionMatrix(mProjectionMatrix);
        immediate.setProjectionMatrix(mProjectionMatrix);
        path.calcWVP();
        immediate.calcWVP();
        immediate.enableLighting(false)
        immediate.enableTexture(false)

        immediate.drawWireCube(75.0f)
        path.color(1f,0f,1f,1f)
        path.setRadius(75.0f)
        if(att>1) {
            Log.v("onDrawFrame", "${att}")
        }
        earth._svgPaths.forEach{
            path.drawStrip(it.points, att.toInt())
        }
        att *= 0.7f
    }
}

