package com.example.myglsurfaceviewapp


import android.util.Log
import android.content.Context
import android.content.res.AssetManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.FileNotFoundException

import kotlin.math.*
import kotlin.random.Random

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
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

data class Vector2(var x: Float, var y: Float)

private val _svgSize = Vector2(0.01f, 0.01f)
fun Conv(lonlat:Vector2):Vector2 {
	return Vector2(lonlat.x/_svgSize.x-0.5f, -lonlat.y/_svgSize.y)
}
data class SvgPath(val points: MutableList<Vector2> = mutableListOf()) {
	fun makePoints(text: String):Boolean {
		val result = mutableListOf<Vector2>()
		val numbers = mutableListOf<Float>()
		
		//Log.v("SvgPathParser", "input: ${text}")
		val input = text.substring(1) // 先頭のMを除去
		val pattern: Pattern = Pattern.compile("-?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?")
		val matcher: Matcher = pattern.matcher(input)
		
		while (matcher.find()) {
			try {
				val number = matcher.group().toFloat()
				//Log.v("svgPathParser","[${numbers.size}]:${number}")
				numbers.add(number)
			} catch (e: NumberFormatException) {
				Log.w("SvgPathParser", "数値のパースに失敗: ${matcher.group()}")
			}
		}
		
		if (numbers.count() < 2) {
			Log.d("SvgPathParser", "Too few numbers in path data")
			return false
		}
		
//		Log.v("makePoints","svgPath=${_svgSize}")
		val pen = Vector2(numbers[0], numbers[1])
		points.add(Conv(pen))
		
		for (i in 2 until numbers.count() - 1 step 2) {
			pen.x += numbers[i + 0]
			pen.y += numbers[i + 1]
			points.add(Conv(pen))
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
				_svgSize.x = widthStr.toFloat()
				_svgSize.y = heightStr.toFloat()
				//Log.v("startElement","svgPath=${_svgSize}")
			}
		}
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

class World98(context: Context) {
	private val svgFile = "World98.svg"
	val _svgPaths = mutableListOf<SvgPath>()
	private val assetManager: AssetManager = context.assets
	
	init {
		parseSVG(svgFile)
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
	
}
