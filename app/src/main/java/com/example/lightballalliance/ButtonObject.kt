package com.example.lightballalliance

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class ButtonObject (
  centerX: Float,
  centerY: Float,
  radius: Float
) {
  private var buttonVertexBuffer: FloatBuffer
  private var buttonProgram: Int = 0
  private var buttonPositionHandle: Int = 0
  private var buttonColorHandle: Int = 0
  private var ratio: Float = 9/16f

  private val buttonFragmentShaderCode =
    "precision mediump float;" +
    "uniform vec4 vColor;" +
    "void main() {" +
    "  gl_FragColor = vColor;" +
    "}"

  private val buttonVertexShaderCode =
    "attribute vec4 vPosition;" +
    "void main() {" +
    "  gl_Position = vPosition;" +
    "}"

  init {
    // Calculate the vertices of the button
    val segments = 100
    val vertices = FloatArray((segments + 2) * 2)
    vertices[0] = centerX
    vertices[1] = centerY

    // Set the radius of the button so that it is always a circle and not an ellipse
    // using the device's screen aspect ratio
    for (i in 0..segments) {
      val theta = (2.0 * Math.PI * i / segments).toFloat()
      vertices[(i + 1) * 2] = centerX + radius * cos(theta) / ratio
      vertices[(i + 1) * 2 + 1] = centerY + radius * sin(theta)
    }

    // Initialize the button vertex buffer
    val bb = ByteBuffer.allocateDirect(vertices.size * 4)
    bb.order(ByteOrder.nativeOrder())
    buttonVertexBuffer = bb.asFloatBuffer()
    buttonVertexBuffer.put(vertices)
    buttonVertexBuffer.position(0)

    // Load and compile the shader programs for the button
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, buttonVertexShaderCode)
    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, buttonFragmentShaderCode)

    // Create and link the button program
    buttonProgram = GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vertexShader)
      GLES20.glAttachShader(it, fragmentShader)
      GLES20.glLinkProgram(it)
    }
  }

  fun draw() {
    // Use the button program
    GLES20.glUseProgram(buttonProgram)

    // Set the position attribute for the button
    buttonPositionHandle = GLES20.glGetAttribLocation(buttonProgram, "vPosition")
    GLES20.glVertexAttribPointer(buttonPositionHandle, 2, GLES20.GL_FLOAT, false, 0, buttonVertexBuffer)
    GLES20.glEnableVertexAttribArray(buttonPositionHandle)

    // Set the color uniform for the button
    buttonColorHandle = GLES20.glGetUniformLocation(buttonProgram, "vColor")
    GLES20.glUniform4fv(buttonColorHandle, 1, floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f), 0)

    // Draw the button
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, (buttonVertexBuffer.capacity() / 2))

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(buttonPositionHandle)
  }

  fun setRatio(ratio: Float) {
    this.ratio = ratio
  }
}