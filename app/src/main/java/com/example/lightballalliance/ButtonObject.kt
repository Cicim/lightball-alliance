package com.example.lightballalliance

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

const val COORDS_PER_VERTEX_2D = 2
const val COORDS_PER_TEXTURE_2D = 2

class ButtonObject (
  private val context: Context,
  private var aspectRatio: Float = 9f / 16f,
  squareSize: Float = 0.1f,
  centerX: Float = 0.0f,
  centerY: Float = -0.9f
) {
  // Coordinates of the square object
  private val squareCoords = floatArrayOf(
    (centerX - squareSize) / aspectRatio, (centerY + squareSize), // top left
    (centerX - squareSize) / aspectRatio, (centerY - squareSize), // bottom left
    (centerX + squareSize) / aspectRatio, (centerY - squareSize), // bottom right
    (centerX + squareSize) / aspectRatio, (centerY + squareSize) // top right
  )

  // Texture coordinates
  private val textureCoords = floatArrayOf(
    0.0f, 0.0f, // top left
    0.0f, 1.0f, // bottom left
    1.0f, 1.0f, // bottom right
    1.0f, 0.0f // top right
  )

  // Number of coordinates per vertex in this array
  private val vertexStride = COORDS_PER_VERTEX_2D * 4 // 4 bytes per vertex
  private val textureStride = COORDS_PER_TEXTURE_2D * 4 // 4 bytes per vertex

  private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

  private var vertexBuffer: FloatBuffer =
    // (number of coordinate values * 4 bytes per float)
    ByteBuffer.allocateDirect(squareCoords.size * 4).run {
      order(ByteOrder.nativeOrder())
      asFloatBuffer().apply {
        put(squareCoords)
        position(0)
      }
    }

  private var textureBuffer: FloatBuffer =
    // (number of coordinate values * 4 bytes per float)
    ByteBuffer.allocateDirect(textureCoords.size * 4).run {
      order(ByteOrder.nativeOrder())
      asFloatBuffer().apply {
        put(textureCoords)
        position(0)
      }
    }

  private val vertexShaderCode =
    "attribute vec4 vPosition;" +
    "attribute vec2 aTexCoord;" +
    "varying vec2 vTexCoord;" +
    "void main() {" +
    "  gl_Position = vPosition;" +
    "  vTexCoord = aTexCoord;" +
    "}"

  private val fragmentShaderCode =
    "precision mediump float;" +
    "uniform sampler2D uTexture;" +
    "varying vec2 vTexCoord;" +
    "void main() {" +
    "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
    "}"

  private var program: Int = 0

  private var positionHandle: Int = 0
  private var textureHandle: Int = 0
  private var textureId: Int = 0

  init {
    val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    program = GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vertexShader)
      GLES20.glAttachShader(it, fragmentShader)
      GLES20.glLinkProgram(it)
    }

    loadTexture()
  }

  fun draw() {
    GLES20.glUseProgram(program)

    positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also {
      GLES20.glEnableVertexAttribArray(it)
      GLES20.glVertexAttribPointer(
        it,
        COORDS_PER_VERTEX_2D,
        GLES20.GL_FLOAT,
        false,
        vertexStride,
        vertexBuffer
      )
    }

    textureHandle = GLES20.glGetAttribLocation(program, "aTexCoord").also {
      GLES20.glEnableVertexAttribArray(it)
      GLES20.glVertexAttribPointer(
        it,
        COORDS_PER_TEXTURE_2D,
        GLES20.GL_FLOAT,
        false,
        textureStride,
        textureBuffer
      )
    }

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    GLES20.glUniform1i(textureHandle, 0)

    GLES20.glDrawElements(
      GLES20.GL_TRIANGLES, drawOrder.size,
      GLES20.GL_UNSIGNED_SHORT, drawListBuffer
    )

    GLES20.glDisableVertexAttribArray(positionHandle)
    GLES20.glDisableVertexAttribArray(textureHandle)
  }

  private fun loadTexture() {
    val textureHandles = IntArray(1)
    GLES20.glGenTextures(1, textureHandles, 0)

    if (textureHandles[0] != 0) {
      val bitmap =
        try {
          BitmapFactory.decodeStream(context.assets.open("shootButton.png"))
        } catch (e: IOException) {
          e.printStackTrace()
          return
        }

      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0])

      GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MIN_FILTER,
        GLES20.GL_LINEAR
      )
      GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MAG_FILTER,
        GLES20.GL_LINEAR
      )

      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

      bitmap.recycle()
    }

    textureId = textureHandles[0]
  }

  private fun loadShader(type: Int, shaderCode: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
      GLES20.glShaderSource(shader, shaderCode)
      GLES20.glCompileShader(shader)
    }
  }

  fun setRatio(ratio: Float) {
    this.aspectRatio = ratio
  }

  private val drawListBuffer = ByteBuffer
    .allocateDirect(squareCoords.size * 2)
    .run {
      order(ByteOrder.nativeOrder())
      asShortBuffer().apply {
        put(drawOrder)
        position(0)
      }
    }
}
