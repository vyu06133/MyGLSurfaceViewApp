package com.example.myglsurfaceviewapp

import ImmediateDraw
import android.util.Log
import android.content.Context
import android.content.res.AssetManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.FileNotFoundException

import kotlin.math.*

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParserFactory

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

data class Vector2(var x: Float, var y: Float)

private val _svgSize = Vector2(0.0f, 0.0f)

data class SvgPath(val points: MutableList<Vector2> = mutableListOf()) {
    private val _svgSize = Vector2(0.0f, 0.0f)
    fun makePoints(text: String):Boolean {
        val result = mutableListOf<Vector2>()
        val numbers = mutableListOf<Float>()
        
        val input = text.substring(1) // 先頭のMを除去
        val pattern: Pattern = Pattern.compile("-?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?")
        val matcher: Matcher = pattern.matcher(input)
        
        while (matcher.find()) {
            try {
                val number = matcher.group().toFloat()
                numbers.add(number)
            } catch (e: NumberFormatException) {
                Log.w("SvgPathParser", "数値のパースに失敗: ${matcher.group()}")
            }
        }
        
        if (numbers.count() < 2) {
            Log.d("SvgPathParser", "Too few numbers in path data")
            return false
        }
        
        val pen = Vector2(numbers[0]*_svgSize.x, numbers[1]*_svgSize.y)
        //Log.v("SvgPathParser", "pen: ${pen}")
        points.add(pen)
        
        var elem = 0
        for (i in 2 until numbers.count() - 1 step 2) {
            pen.x += numbers[i]
            pen.y += numbers[i + 1]
            //Log.v("SvgPathParser", "pen: ${pen}")
            points.add(Vector2(pen.x*_svgSize.x, pen.y*_svgSize.y))
        }
        return true
    }
}

class PathOnlySvgSAXHandler : DefaultHandler() {
    val paths = mutableListOf<SvgPath>()
    
    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        when (qName) {
            "path" -> {
                var p = SvgPath()
                p.makePoints(attributes!!.getValue("d"))
                paths.add(p)
            }
            "svg"->{
                val widthStr = attributes!!.getValue("width")  // "8e3"
                val heightStr = attributes!!.getValue("height") // "3859"
                Log.v("svgSize", "width=${widthStr} height=${heightStr}")
                _svgSize.x = 1.0f/widthStr.toFloat()
                _svgSize.y = 1.0f/heightStr.toFloat()
            }//todo:正常か確認する
        }
    }
    
    override fun endDocument() {
        Log.d("SvgSAX", "SVG Document Parsed. Found ${paths.size} path elements.")
        var pathsCount = paths.count()
//        for(i in 0 until pathsCount) {
//           Log.v("Parse", "i=${i} pathsCount=${pathsCount}")
//            for (j in 0 until paths[i].points.count()) {
//                paths[i].points[j].x = paths[i].points[j].x * _svgSize.x
//                paths[i].points[j].y = -(0.5f - paths[i].points[j].y * _svgSize.y)
//            }
//        }
    }
}

fun parsePathOnlySvgWithSax(inputStream: InputStream): PathOnlySvgSAXHandler {
    val factory = SAXParserFactory.newInstance()
    val saxParser = factory.newSAXParser()
    val handler = PathOnlySvgSAXHandler()
    saxParser.parse(inputStream, handler)
    return handler
}

// assetsフォルダのSVGファイルを読み込み、path要素のみを抽出する例
fun readPathOnlySvgFromAssetsWithSax(
    assetManager: android.content.res.AssetManager,
    fileName: String
): PathOnlySvgSAXHandler? {
    return try {
        val inputStream = assetManager.open(fileName)
        val handler = parsePathOnlySvgWithSax(inputStream)
        inputStream.close()
        handler
    } catch (e: Exception) {
        Log.e("SvgRead", "AssetsからのSVG読み込みエラー (Path Only SAX): ${e.message}")
        null
    }
}

class Earth(context: Context)
{
    private val svgFile = "World98.svg"
    private val _svgPaths = mutableListOf<SvgPath>()
    private val assetManager: AssetManager = context.assets
    
    private val immediate = ImmediateDraw()
    private val vertexBuffer: FloatBuffer
//    private val colorBuffer: FloatBuffer
    private val vertexCount = 36

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
			float phi = (vPosition.y * 180.0f) * 3.14f / 180.0f;
			float lambda = (vPosition.x * 360.0f) * 3.14f / 180.0f;
            float radius = 1.0f;

			vec4 pos=vPosition;
			//vec4 pos;
            //pos.x = radius * cos(phi) * cos(lambda);
            //pos.y = radius * sin(phi);
			//pos.z = radius * cos(phi) * sin(lambda);
            //pos.w = 1.0f;

            gl_Position = uMVPMatrix * pos;
            vColor = aColor;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vec4(1,1,1,1);
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

    private fun cossin( rad:Float, ang:Float ):Vector2
    {
        return Vector2(cos(ang)*rad,sin(ang)*rad);
    }
    
    init {
        parseSVG(svgFile)
        
        vertexBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(cubeCoords)
                position(0)
            }

//        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
//            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
//                put(colors)
//                position(0)
//            }

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }
private var frameTime:Float=0.0f
    fun draw(mvpMatrix: FloatArray)
    {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
//        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        
        frameTime=frameTime+0.01f
if(false) {
    var vi: Int = 0
    _svgPaths.forEach {
        var sz = it.points.size
        for (i in it.points.indices) {
            if (vi < 36/*vertexCount*/) {
                vertexBuffer.put(vi * 3 + 0, it.points.get(i).x)
                vertexBuffer.put(vi * 3 + 1, it.points.get(i).y)
                vertexBuffer.put(vi * 3 + 2, it.points.get(i).x+it.points.get(i).y)
                vi++;
                //if(vi>=10)break
            }
        }
    }
}

if(false)
{
    var v1=cossin(1.0f,-30.0f*180.0f/3.14f)
    var v2=cossin(1.0f,210.0f*180.0f/3.14f)
    var v3=cossin(1.0f,frameTime)
    vertexBuffer.put(0,v1.x)
    vertexBuffer.put(1,v1.y)
    vertexBuffer.put(2,0.0f)
    vertexBuffer.put(3,v2.x)
    vertexBuffer.put(4,v2.y)
    vertexBuffer.put(5,0.0f)
    vertexBuffer.put(6,v3.x)
    vertexBuffer.put(7,v3.y)
    vertexBuffer.put(8,0.0f)
    vertexBuffer.put(9,v1.x)
    vertexBuffer.put(10,v1.y)
    vertexBuffer.put(11,0.0f)
}
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

//        GLES20.glEnableVertexAttribArray(colorHandle)
//        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

//        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
//        GLES20.glDisableVertexAttribArray(colorHandle)
        immediate.setMVP(mvpMatrix)
        immediate.setLight(
            floatArrayOf(1f, 3f, 2f),
            floatArrayOf(0.1f, 0.1f, 0.1f),
            floatArrayOf(0.6f, 0.6f, 0.6f),
            floatArrayOf(1f, 1f, 1f))
//        immediate.setLightDir(1f, 3f, 2f)
//        immediate.setMaterial(
//            ambient = floatArrayOf(0.1f, 0.1f, 0.1f),
//            diffuse = floatArrayOf(0.6f, 0.6f, 0.6f),
//            specular = floatArrayOf(1f, 1f, 1f),
//            shininess = 64f
//        )
        immediate.enableTexture(false) // テクスチャ無効化も可能

        immediate.begin(GLES20.GL_TRIANGLES)
        immediate.normal(0f, 0f, 1f)
        immediate.vertex( -1f,  1f,  1f)
        immediate.vertex(-1f, -1f,  1f)
        immediate.vertex(1f, -1f,  1f)
        immediate.vertex(-1f,  1f,  1f)
        immediate.vertex(1f, -1f,  1f)
        immediate.vertex(1f,  1f,  1f)
        // 背面
        immediate.normal(0f, 0f, -1f)
        immediate.vertex(-1f,  1f, -1f)
        immediate.vertex(1f, -1f, -1f)
        immediate.vertex(-1f, -1f, -1f)
        immediate.vertex(-1f,  1f, -1f)
        immediate.vertex(1f,  1f, -1f)
        immediate.vertex(1f, -1f, -1f)
        // 左面
        immediate.normal(-1f, 0f, 0f)
        immediate.vertex(-1f,  1f, -1f)
        immediate.vertex(-1f, -1f, -1f)
        immediate.vertex(-1f, -1f,  1f)
        immediate.vertex(-1f,  1f, -1f)
        immediate.vertex(-1f, -1f,  1f)
        immediate.vertex(-1f,  1f,  1f)
        // 右面
        immediate.normal(1f, 0f, 0f)
        immediate.vertex(1f,  1f, -1f)
        immediate.vertex(1f, -1f,  1f)
        immediate.vertex(1f, -1f, -1f)
        immediate.vertex(1f,  1f, -1f)
        immediate.vertex(1f,  1f,  1f)
        immediate.vertex(1f, -1f,  1f)
        // 上面
        immediate.normal(0f, 1f, 0f)
        immediate.vertex(-1f,  1f, -1f)
        immediate.vertex(-1f,  1f,  1f)
        immediate.vertex(1f,  1f,  1f)
        immediate.vertex(-1f,  1f, -1f)
        immediate.vertex(1f,  1f,  1f)
        immediate.vertex(1f,  1f, -1f)
        // 下面
        immediate.normal(0f, -1f, 0f)
        immediate.vertex(-1f, -1f, -1f)
        immediate.vertex(1f, -1f,  1f)
        immediate.vertex(-1f, -1f,  1f)
        immediate.vertex(-1f, -1f, -1f)
        immediate.vertex(1f, -1f, -1f)
        immediate.vertex(1f, -1f,  1f)
        
        immediate.end()
    }

    private fun loadShader(type: Int, shaderCode: String): Int
    {
        return GLES20.glCreateShader(type).also {
                shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun parseSVG(src: String): Boolean {
        val pathHandler = readPathOnlySvgFromAssetsWithSax(assetManager, src)
        pathHandler?.paths?.forEach {
            //Log.d("SvgPathData","svgpath: ${it.points}")
            _svgPaths.add(it)
            // ここで pathData を解析して LineStrip オブジェクトを生成するなどの処理を行う
        }
        return true
    }
    
    
/*
    class SvgPathParser(private val svgWidth: Float, private val svgHeight: Float) {
 
        
        private val invW: Float = if (svgWidth != 0f) 1.0f / svgWidth else 0f
        private val invH: Float = if (svgHeight != 0f) 1.0f / svgHeight else 0f
        
        fun parseDAttribute(attr: String): List<Vector2> {
            val result = mutableListOf<Vector2>()
            val numbers = mutableListOf<Float>()
            
            val input = attr.substring(1) // 先頭のMを除去
            val pattern: Pattern = Pattern.compile("-?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?")
            val matcher: Matcher = pattern.matcher(input)
            
            while (matcher.find()) {
                try {
                    val number = matcher.group().toFloat()
                    numbers.add(number)
                } catch (e: NumberFormatException) {
                    Log.w("SvgPathParser", "数値のパースに失敗: ${matcher.group()}")
                }
            }
            
            if (numbers.count() < 2) {
                Log.d("SvgPathParser", "Too few numbers in path data")
                return result
            }
            
            val xy = Vector2(0.0f, 0.0f)
            val pen = Vector2(numbers[0], numbers[1])
            result.add(pen)
            
            var elem = 0
            for (i in 2 until numbers.count()) {
                if (elem == 0) {
                    xy.x = numbers[i]
                    elem++
                } else {
                    xy.y = numbers[i]
                    elem = 0
                    pen.x += xy.x
                    pen.y += xy.y
                    result.add(Vector2(pen.x, pen.y))
                }
            }
            
            for (i in 0 until result.count()) {
                val temp = result[i]
                temp.x *= invW
                temp.y *= invH
                temp.y = -(0.5f - temp.y)
                result[i] = temp
            }
            
            return result
        }
    }
    
 */
}

