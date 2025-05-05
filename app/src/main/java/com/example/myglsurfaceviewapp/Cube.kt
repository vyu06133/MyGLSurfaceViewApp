package com.example.myglsurfaceviewapp



import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Cube
{
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val vertexCount = 36

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vColor = aColor;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    private val cubeCoords = floatArrayOf(
        // 前面
        -1f,  1f,  1f,   -1f, -1f,  1f,    1f, -1f,  1f,
        -1f,  1f,  1f,    1f, -1f,  1f,    1f,  1f,  1f,
        // 背面
        -1f,  1f, -1f,    1f, -1f, -1f,   -1f, -1f, -1f,
        -1f,  1f, -1f,    1f,  1f, -1f,    1f, -1f, -1f,
        // 左面
        -1f,  1f, -1f,   -1f, -1f, -1f,   -1f, -1f,  1f,
        -1f,  1f, -1f,   -1f, -1f,  1f,   -1f,  1f,  1f,
        // 右面
        1f,  1f, -1f,    1f, -1f,  1f,    1f, -1f, -1f,
        1f,  1f, -1f,    1f,  1f,  1f,    1f, -1f,  1f,
        // 上面
        -1f,  1f, -1f,   -1f,  1f,  1f,    1f,  1f,  1f,
        -1f,  1f, -1f,    1f,  1f,  1f,    1f,  1f, -1f,
        // 下面
        -1f, -1f, -1f,    1f, -1f,  1f,   -1f, -1f,  1f,
        -1f, -1f, -1f,    1f, -1f, -1f,    1f, -1f,  1f,
    )

    private val colors = FloatArray(36 * 4) { i ->
        val face = i / 6
        when (face % 6) {
            0 -> floatArrayOf(1f, 0f, 0f, 1f) // 赤
            1 -> floatArrayOf(0f, 1f, 0f, 1f) // 緑
            2 -> floatArrayOf(0f, 0f, 1f, 1f) // 青
            3 -> floatArrayOf(1f, 1f, 0f, 1f) // 黄
            4 -> floatArrayOf(0f, 1f, 1f, 1f) // シアン
            else -> floatArrayOf(1f, 0f, 1f, 1f) // マゼンタ
        }[i % 4]
    }

    private val program: Int

    init {
        vertexBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(cubeCoords)
                position(0)
            }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(colors)
                position(0)
            }

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    fun draw(mvpMatrix: FloatArray)
    {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int
    {
        return GLES20.glCreateShader(type).also {
                shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}

