package com.example.lightballalliance

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class HealthBarObject (
  private val context: Context,
  aspectRatio: Float,
  private val textureName: String,
  squareSize: Float = 0.03f,
  centerX: Float,
  centerY: Float
) {
  // Coordinates of the square object
  private val squareCoords = floatArrayOf(
    (centerX - squareSize / 2) / aspectRatio, (centerY + squareSize / 2), // top left
    (centerX - squareSize / 2) / aspectRatio, (centerY - squareSize / 2), // bottom left
    (centerX + squareSize / 2) / aspectRatio, (centerY - squareSize / 2), // bottom right
    (centerX + squareSize / 2) / aspectRatio, (centerY + squareSize / 2) // top right
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

  // Render the health bar until the health value if the pixels are different than white
  private val fragmentShaderCode =
    "precision mediump float;" +
    "uniform sampler2D uTexture;" +
    "varying vec2 vTexCoord;" +
    "uniform float vHealth;" +
    "void main() {" +
    "  vec4 color = texture2D(uTexture, vTexCoord);" +
    "  if (vTexCoord.x < vHealth || color == vec4(1.0, 1.0, 1.0, 1.0)) {" +
    "    gl_FragColor = color;" +
    "  } else {" +
    "    gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);" +
    "  }" +
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

  fun draw(health: Int = 100) {
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
    GLES20.glGetUniformLocation(program, "vHealth").also { healthHandle ->
      GLES20.glUniform1f(healthHandle, health / 100f)
    }

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
          BitmapFactory.decodeStream(context.assets.open(textureName))
        } catch (e: IOException) {
          e.printStackTrace()
          return
        }

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

    textureId = textureHandles[0]
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
