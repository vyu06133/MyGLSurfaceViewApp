package com.example.myglsurfaceviewapp


import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer
{
    private lateinit var cube: Cube

    private val mVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mModelMatrix = FloatArray(16)

    var angleX = 0f
    var angleY = 0f

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?)
    {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        cube = Cube()
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int)
    {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    override fun onDrawFrame(unused: GL10?)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(mViewMatrix, 0,
            0f, 0f, 5f,
            0f, 0f, 0f,
            0f, 1.0f, 0.0f)

        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.rotateM(mModelMatrix, 0, angleX, 1f, 0f, 0f)
        Matrix.rotateM(mModelMatrix, 0, angleY, 0f, 1f, 0f)

        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
        val finalMatrix = FloatArray(16)
        Matrix.multiplyMM(finalMatrix, 0, mVPMatrix, 0, mModelMatrix, 0)

        cube.draw(finalMatrix)
    }
}

