package com.example.myglsurfaceviewapp


import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ImmediateDraw(
	private val maxVertices: Int = 1024
) {
	private val floatsPerVertex = 3 + 4 + 3 + 2
	private val stride = floatsPerVertex * 4
	
	private lateinit var vertexBuffer: FloatBuffer
	private var vertexCount = 0
	private var primitiveType = GLES20.GL_TRIANGLES
	
	private var currentColor = floatArrayOf(1f, 1f, 1f, 1f)
	private var currentNormal = floatArrayOf(0f, 0f, 1f)
	private var currentUV = floatArrayOf(0f, 0f)
	
	private var program = 0
	private var aPosition = 0
	private var aColor = 0
	private var aNormal = 0
	private var aTexCoord = 0
	private var uMVPMatrix = 0
	private var uLightDir = 0
	private var uAmbient = 0
	private var uDiffuse = 0
	private var uSpecular = 0
	private var uShininess  = 0
	private var uUseTexture = 0
	private var uEnableLighting = 0
	
	private val worldMatrix = FloatArray(16)
	private val viewMatrix = FloatArray(16)
	private val projectionMatrix = FloatArray(16)
	private val wvpMatrix = FloatArray(16)
	
	private var enLighting = true
	private var useTexture = false
	private var lightDir = floatArrayOf(0f, 0f, 1f)
	private var ambient = floatArrayOf(0.2f, 0.2f, 0.2f)
	private var diffuse = floatArrayOf(0.7f, 0.7f, 0.7f)
	private var specular = floatArrayOf(0.3f, 0.3f, 0.3f)
	private var shininess = 64f
	
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
	
	fun normal(x: Float, y: Float, z: Float) {
		currentNormal = floatArrayOf(x, y, z)
	}
	
	fun texCoord(u: Float, v: Float) {
		currentUV = floatArrayOf(u, v)
	}
	
	fun vertex(x: Float, y: Float, z: Float) {
		if (vertexCount >= maxVertices) throw RuntimeException("Vertex buffer overflow")
		vertexBuffer.put(x).put(y).put(z)
		vertexBuffer.put(currentColor)
		vertexBuffer.put(currentNormal)
		vertexBuffer.put(currentUV)
		vertexCount++
	}
	
	fun setWorldMatrix(matrix: FloatArray, recalcWVP: Boolean = false) {
		if (matrix.size != 16) throw IllegalArgumentException("world matrix must be 4x4")
		System.arraycopy(matrix, 0, worldMatrix, 0, 16)
		//Log.v("ImmediateDraw","worldMatrix=${worldMatrix.contentToString()}")
		if(recalcWVP) {
			calcWVP()
		}
	}
	
	fun setViewMatrix(matrix: FloatArray, recalcWVP: Boolean = false) {
		if (matrix.size != 16) throw IllegalArgumentException("view matrix must be 4x4")
		System.arraycopy(matrix, 0, viewMatrix, 0, 16)
		//Log.v("ImmediateDraw","viewMatrix=${viewMatrix.contentToString()}")
		if(recalcWVP) {
			calcWVP()
		}
	}
	
	fun setProjectionMatrix(matrix: FloatArray, recalcWVP: Boolean = false) {
		if (matrix.size != 16) throw IllegalArgumentException("projection matrix must be 4x4")
		System.arraycopy(matrix, 0, projectionMatrix, 0, 16)
		//Log.v("ImmediateDraw","projectionMatrix=${projectionMatrix.contentToString()}")
		if(recalcWVP) {
			calcWVP()
		}
	}
	
	fun calcWVP(){
		val vp = FloatArray(16)
		Matrix.multiplyMM(vp, 0, projectionMatrix, 0, viewMatrix, 0)
		Matrix.multiplyMM(wvpMatrix, 0, vp, 0, worldMatrix, 0)
		//Log.v("ImmediateDraw","wvpMatrix=${wvpMatrix.contentToString()}")
	}
	
	fun enableTexture(enabled: Boolean) {
		useTexture = enabled
	}

	fun enableLighting(enabled: Boolean) {
		enLighting = enabled
	}
	
	fun setLightDir(x: Float, y: Float, z: Float) {
		lightDir = floatArrayOf(x, y, z)
	}
	
	fun setLight(amb: FloatArray, diff: FloatArray, spec: FloatArray, value:Float) {
		ambient = amb
		diffuse = diff
		specular = spec
		shininess = value
	}
	
	fun end() {
		if (vertexCount == 0) return
		
		vertexBuffer.position(0)
		GLES20.glUseProgram(program)
		
		GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, wvpMatrix, 0)
		GLES20.glUniform3fv(uLightDir, 1, lightDir, 0)
		GLES20.glUniform3fv(uAmbient, 1, ambient, 0)
		GLES20.glUniform3fv(uDiffuse, 1, diffuse, 0)
		GLES20.glUniform3fv(uSpecular, 1, specular, 0)
		GLES20.glUniform1f(uShininess, shininess)
		GLES20.glUniform1i(uUseTexture, if (useTexture) 1 else 0)
		GLES20.glUniform1i(uEnableLighting, if (enLighting) 1 else 0)
		
		GLES20.glEnableVertexAttribArray(aPosition)
		GLES20.glEnableVertexAttribArray(aColor)
		GLES20.glEnableVertexAttribArray(aNormal)
		GLES20.glEnableVertexAttribArray(aTexCoord)
		
		vertexBuffer.position(0)
		GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
		vertexBuffer.position(3)
		GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)
		vertexBuffer.position(7)
		GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
		vertexBuffer.position(10)
		GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
		
		GLES20.glDrawArrays(primitiveType, 0, vertexCount)
		
		GLES20.glDisableVertexAttribArray(aPosition)
		GLES20.glDisableVertexAttribArray(aColor)
		GLES20.glDisableVertexAttribArray(aNormal)
		GLES20.glDisableVertexAttribArray(aTexCoord)
	}
	
	private fun setupDefaultShader() {
		val vertexShaderCode = """
            uniform mat4 u_MVPMatrix;
            attribute vec3 a_Position;
            attribute vec4 a_Color;
            attribute vec3 a_Normal;
            attribute vec2 a_TexCoord;

            varying vec4 v_Color;
            varying vec3 v_Normal;
            varying vec2 v_TexCoord;

            void main() {
                gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
                v_Color = a_Color;
                v_Normal = normalize(a_Normal);
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()
		
		val fragmentShaderCode = """
            precision mediump float;

            uniform bool u_EnableLighting;
            uniform vec3 u_LightDir;
            uniform vec3 u_Ambient;
            uniform vec3 u_Diffuse;
            uniform vec3 u_Specular;
			uniform float u_Shininess;
            uniform bool u_UseTexture;
            uniform sampler2D u_Texture;

            varying vec4 v_Color;
            varying vec3 v_Normal;
            varying vec2 v_TexCoord;

            void main() {
                vec3 norm = normalize(v_Normal);
                
                float diff = 1.0;
                vec3 reflectDir;
                vec3 viewDir;
				float spec = 0.0;
				if(u_EnableLighting) {
					vec3 lightDir = normalize(u_LightDir);
	
	                float diff = max(dot(norm, lightDir), 0.0);
	                vec3 reflectDir = reflect(-lightDir, norm);
	                vec3 viewDir = vec3(0.0, 0.0, 1.0);
					float spec = pow(max(dot(reflectDir, viewDir), 0.0), u_Shininess);
				}


                vec3 ambient = u_Ambient * v_Color.rgb;
                vec3 diffuse = u_Diffuse * diff * v_Color.rgb;
                vec3 specular = u_Specular * spec;

                vec4 baseColor = v_Color;
                if (u_UseTexture) {
                    baseColor *= texture2D(u_Texture, v_TexCoord);
                }

                gl_FragColor = vec4(ambient + diffuse + specular, baseColor.a);
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
		aNormal = GLES20.glGetAttribLocation(program, "a_Normal")
		aTexCoord = GLES20.glGetAttribLocation(program, "a_TexCoord")
		uMVPMatrix = GLES20.glGetUniformLocation(program, "u_MVPMatrix")
		uLightDir = GLES20.glGetUniformLocation(program, "u_LightDir")
		uAmbient = GLES20.glGetUniformLocation(program, "u_Ambient")
		uDiffuse = GLES20.glGetUniformLocation(program, "u_Diffuse")
		uSpecular = GLES20.glGetUniformLocation(program, "u_Specular")
		uShininess = GLES20.glGetUniformLocation(program, "u_Shininess")
		uUseTexture = GLES20.glGetUniformLocation(program, "u_UseTexture")
		uEnableLighting = GLES20.glGetUniformLocation(program, "u_EnableLighting")
	}
	
	private fun loadShader(type: Int, code: String): Int {
		return GLES20.glCreateShader(type).also { shader ->
			GLES20.glShaderSource(shader, code)
			GLES20.glCompileShader(shader)
		}
	}

	fun drawSolidCube(size:Float=1.0f)
	{
		begin(GLES20.GL_TRIANGLES)
		// 前面
		color(0f,0f,1f,0.5f)
		normal(0f, 0f, 1f)
		vertex( -size,  size,  size)
		vertex(-size, -size,  size)
		vertex(size, -size,  size)
		vertex(-size,  size,  size)
		vertex(size, -size,  size)
		vertex(size,  size,  size)
		// 背面
		color(0f,1f,0f,0.5f)
		normal(0f, 0f, -1f)
		vertex(-size,  size, -size)
		vertex(size, -size, -size)
		vertex(-size, -size, -size)
		vertex(-size,  size, -size)
		vertex(size,  size, -size)
		vertex(size, -size, -size)
		// 左面
		color(0f,1f,1f,0.5f)
		normal(-1f, 0f, 0f)
		vertex(-size,  size, -size)
		vertex(-size, -size, -size)
		vertex(-size, -size,  size)
		vertex(-size,  size, -size)
		vertex(-size, -size,  size)
		vertex(-size,  size,  size)
		// 右面
		color(1f,0f,0f,0.5f)
		normal(1f, 0f, 0f)
		vertex(size,  size, -size)
		vertex(size, -size,  size)
		vertex(size, -size, -size)
		vertex(size,  size, -size)
		vertex(size,  size,  size)
		vertex(size, -size,  size)
		// 上面
		color(1f,0f,1f,0.5f)
		normal(0f, 1f, 0f)
		vertex(-size,  size, -size)
		vertex(-size,  size,  size)
		vertex(size,  size,  size)
		vertex(-size,  size, -size)
		vertex(size,  size,  size)
		vertex(size,  size, -size)
		// 下面
		color(1f,1f,0f,0.5f)
		normal(0f, -1f, 0f)
		vertex(-size, -size, -size)
		vertex(size, -size,  size)
		vertex(-size, -size,  size)
		vertex(-size, -size, -size)
		vertex(size, -size, -size)
		vertex(size, -size,  size)
		end()
	}
	fun drawWireCube(size:Float=1.0f)
	{
		begin(GLES20.GL_LINES)
		vertex(-size, size, size)
		vertex(size, size, size)    // 線1 (A-B)
		vertex(size, size, size)
		vertex(size, -size, size)   // 線2 (B-C)
		vertex(size, -size, size)
		vertex(-size, -size, size)  // 線3 (C-D)
		vertex(-size, -size, size)
		vertex(-size, size, size)   // 線4 (D-A)
		vertex(-size, size, -size)
		vertex(size, size, -size)   // 線5 (E-F)
		vertex(size, size, -size)
		vertex(size, -size, -size)  // 線6 (F-G)
		vertex(size, -size, -size)
		vertex(-size, -size, -size) // 線7 (G-H)
		vertex(-size, -size, -size)
		vertex(-size, size, -size)  // 線8 (H-E)
		vertex(-size, size, size)
		vertex(-size, size, -size)  // 線9 (A-E)
		vertex(size, size, size)
		vertex(size, size, -size)   // 線10 (B-F)
		vertex(size, -size, size)
		vertex(size, -size, -size)  // 線11 (C-G)
		vertex(-size, -size, size)
		vertex(-size, -size, -size)  // 線12 (D-H)
		end()

	}
}
