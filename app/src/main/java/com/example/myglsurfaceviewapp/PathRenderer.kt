package com.example.myglsurfaceviewapp


import android.util.Log
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import com.example.myglsurfaceviewapp.Vector2
//data class Vector2(var x: Float, var y: Float)

class PathRenderer(
	private val maxVertices: Int = 1024
) {
	private val floatsPerVertex = 2 + 4 // x, y, r, g, b, a
	private val stride = floatsPerVertex * 4
	
	private lateinit var vertexBuffer: FloatBuffer
	private var vertexCount = 0
	private var primitiveType = GLES20.GL_TRIANGLES
	
	private var currentColor = floatArrayOf(1f, 1f, 1f, 1f)
	
	private var program = 0
	private var aPosition = 0
	private var aColor = 0
	private var uMVPMatrix = 0
	private var uRadius = 0
	
	private val worldMatrix = FloatArray(16)
	private val viewMatrix = FloatArray(16)
	private val projectionMatrix = FloatArray(16)
	private val wvpMatrix = FloatArray(16)
	private var radius = 1.0f
	
	init {
		val buffer = ByteBuffer.allocateDirect(maxVertices * stride)
		buffer.order(ByteOrder.nativeOrder())
		vertexBuffer = buffer.asFloatBuffer()
		
		for (i in wvpMatrix.indices) wvpMatrix[i] = if (i % 5 == 0) 1f else 0f
		
		setupDefaultShader()
	}
	
	fun begin(primitive: Int) {
		primitiveType = primitive
		vertexCount = 0
		vertexBuffer.clear()
	}
	
	fun color(r: Float, g: Float, b: Float, a: Float) {
		currentColor = floatArrayOf(r, g, b, a)
	}
	
	fun vertex(x: Float, y: Float) {
		if (vertexCount >= maxVertices) return
		if (vertexCount >= maxVertices) throw RuntimeException("Vertex buffer overflow")
		vertexBuffer.put(x).put(y)
		vertexBuffer.put(currentColor)
		vertexCount++
	}
	
	fun setWorldMatrix(matrix: FloatArray, recalcWVP: Boolean = false) {
		if (matrix.size != 16) throw IllegalArgumentException("world matrix must be 4x4")
		System.arraycopy(matrix, 0, worldMatrix, 0, 16)
		//Log.v("PathRenderer","worldMatrix=${worldMatrix.contentToString()}")
		if(recalcWVP) {
			calcWVP()
		}
	}
	
	fun setViewMatrix(matrix: FloatArray, recalcWVP: Boolean = false) {
		if (matrix.size != 16) throw IllegalArgumentException("view matrix must be 4x4")
		System.arraycopy(matrix, 0, viewMatrix, 0, 16)
		//Log.v("PathRenderer","viewMatrix=${viewMatrix.contentToString()}")
		if(recalcWVP) {
			calcWVP()
		}
	}
	
	fun setProjectionMatrix(matrix: FloatArray, recalcWVP: Boolean = false) {
		if (matrix.size != 16) throw IllegalArgumentException("projection matrix must be 4x4")
		System.arraycopy(matrix, 0, projectionMatrix, 0, 16)
		//Log.v("PathRenderer","projectionMatrix=${projectionMatrix.contentToString()}")
		if(recalcWVP) {
			calcWVP()
		}
	}
	
	fun calcWVP(){
		val vp = FloatArray(16)
		Matrix.multiplyMM(vp, 0, projectionMatrix, 0, viewMatrix, 0)
		Matrix.multiplyMM(wvpMatrix, 0, vp, 0, worldMatrix, 0)
		//Log.v("PathRenderer","wvpMatrix=${wvpMatrix.contentToString()}")
	}
	
	fun setRadius(r:Float)
	{
		radius = r
	}
	
	fun end() {
		if (vertexCount == 0) return
		
		vertexBuffer.position(0)
		GLES20.glUseProgram(program)
		
		GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, wvpMatrix, 0)
		GLES20.glUniform1f(uRadius,radius)
		
		GLES20.glEnableVertexAttribArray(aPosition)
		GLES20.glEnableVertexAttribArray(aColor)
		
		vertexBuffer.position(0)
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
		vertexBuffer.position(2)
		GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)
		
		GLES20.glDrawArrays(primitiveType, 0, vertexCount)
		
		GLES20.glDisableVertexAttribArray(aPosition)
		GLES20.glDisableVertexAttribArray(aColor)
	}
	
	private fun setupDefaultShader() {
		val vertexShaderCode = """
            uniform mat4 u_MVPMatrix;
			uniform float u_radius;
            attribute vec2 a_Position;
            attribute vec4 a_Color;
            varying vec4 v_Color;

			vec3 MapOntoSphere(float lat, float lon)
			{
		        float phi = (lat * 180.0f - 90.0f) * 3.14f / 180.0f;   // 緯度 φ
                float lambda = (lon * 360.0f - 180.0f) * 3.14f / 180.0f; // 経度 λ
				vec3 v;
				v.x = u_radius * sin(phi);
				v.y = u_radius * cos(phi) * cos(lambda);
				v.z = u_radius * cos(phi) * sin(lambda);
				return v;
			}
			
            void main() {
                //gl_Position = u_MVPMatrix * vec4(a_Position * u_radius, 0.0, 1.0);
                gl_Position = u_MVPMatrix * vec4(MapOntoSphere(a_Position.x, a_Position.y), 1.0);
                v_Color = a_Color;
            }
        """.trimIndent()
		
		val fragmentShaderCode = """
            precision mediump float;
            varying vec4 v_Color;

            void main() {
                gl_FragColor = v_Color;
            }
        """.trimIndent()
		
		val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
		val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
		
		program = GLES20.glCreateProgram().also {
			GLES20.glAttachShader(it, vertexShader)
			GLES20.glAttachShader(it, fragmentShader)
			GLES20.glLinkProgram(it)
		}
		
		aPosition = GLES20.glGetAttribLocation(program, "a_Position")
		aColor = GLES20.glGetAttribLocation(program, "a_Color")
		uMVPMatrix = GLES20.glGetUniformLocation(program, "u_MVPMatrix")
		uRadius = GLES20.glGetUniformLocation(program, "u_radius")
	}
	
	private fun loadShader(type: Int, code: String): Int {
		return GLES20.glCreateShader(type).also { shader ->
			GLES20.glShaderSource(shader, code)
			GLES20.glCompileShader(shader)
		}
	}
	
	fun drawRectangle(x: Float, y: Float, width: Float, height: Float) {
		begin(GLES20.GL_TRIANGLE_STRIP)
		vertex(x, y)
		vertex(x + width, y)
		vertex(x, y + height)
		vertex(x + width, y + height)
		end()
	}
	
	fun drawStrip(points:MutableList<Vector2>, s:Int ) {
		if (points.size < 2) return // 線を描画するには少なくとも2つの点が必要
		
		var S=s
		if(S<1)S=1
		begin(GLES20.GL_LINE_STRIP)
		vertex(points[0].x-0.5f, points[0].y)
		for (i in S until points.count() - S step S) {
			if (i > points.count()-1)break;
			var point = points[i]
			vertex(point.x-0.5f, point.y)
		}
//		for (point in points) {
//			vertex(point.x, point.y)
//		}
		end()
	}

	fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
		begin(GLES20.GL_LINES)
		vertex(x1, y1)
		vertex(x2, y2)
		end()
	}
	
	fun drawPoint(x: Float, y: Float, size: Float = 1f) {
//		GLES20.glPointSize(size)
		begin(GLES20.GL_POINTS)
		vertex(x, y)
		end()
//		GLES20.glPointSize(1f) // デフォルトに戻す
	}
}