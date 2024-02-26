package com.example.lightballalliance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextRenderer (
  private var aspectRatio: Float,
  text: String,
  centerX: Float,
  centerY: Float
) {
  // Coordinates of the square object
  private val squareCoords = floatArrayOf(
    (centerX - 0.15f) / aspectRatio, (centerY + 0.05f), // top left
    (centerX - 0.15f) / aspectRatio, (centerY - 0.05f), // bottom left
    (centerX + 0.15f) / aspectRatio, (centerY - 0.05f), // bottom right
    (centerX + 0.15f) / aspectRatio, (centerY + 0.05f) // top right
  )

  // Texture coordinates
  private val textureCoords = floatArrayOf(
    0.0f, 0.0f, // top left
    0.0f, 1.0f, // bottom left
    1.0f, 1.0f, // bottom right
    1.0f, 0.0f // top right
  )

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
    "uniform vec4 vColor;" +
    "void main() {" +
    "  gl_FragColor = vColor * texture2D(uTexture, vTexCoord);" +
    "}"

  private var program: Int = 0

  private var positionHandle: Int = 0
  private var textureHandle: Int = 0
  private var textureId: Int = 0
  private val paint: Paint = Paint()

  init {
    val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    program = GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vertexShader)
      GLES20.glAttachShader(it, fragmentShader)
      GLES20.glLinkProgram(it)
    }

    paint.color = Color.WHITE
    paint.textSize = 30f
    paint.typeface = Typeface.DEFAULT

    setText(text)
  }

  fun setText(text: String) {
    val bitmap = renderTextToBitmap(text)
    textureId = loadTexture(bitmap)
  }

  fun draw(color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)) {
    GLES20.glUseProgram(program)

    positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also {
      GLES20.glEnableVertexAttribArray(it)
      GLES20.glVertexAttribPointer(
        it,
        COORDS_PER_VERTEX_2D,
        GLES20.GL_FLOAT,
        false,
        VERTEX_STRIDE_2D,
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
        TEXTURE_STRIDE_2D,
        textureBuffer
      )
    }

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    GLES20.glUniform1i(textureHandle, 0)

    // Set color for drawing the object
    GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
      GLES20.glUniform4fv(colorHandle, 1, color, 0)
    }

    GLES20.glDrawElements(
      GLES20.GL_TRIANGLES, drawOrder.size,
      GLES20.GL_UNSIGNED_SHORT, drawListBuffer
    )

    GLES20.glDisableVertexAttribArray(positionHandle)
    GLES20.glDisableVertexAttribArray(textureHandle)
  }

  private fun renderTextToBitmap(text: String): Bitmap {
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val width = 150
    val height = 50

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawText(text, (width - bounds.width()) / 2f, height / 2f, paint)

    return bitmap
  }

  private fun loadTexture(bitmap: Bitmap): Int {
    val textureHandles = IntArray(1)
    GLES20.glGenTextures(1, textureHandles, 0)

    if (textureHandles[0] != 0) {
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0])

      GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MIN_FILTER,
        GLES20.GL_NEAREST
      )
      GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MAG_FILTER,
        GLES20.GL_LINEAR
      )

      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

      bitmap.recycle()
    }

    return textureHandles[0]
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
