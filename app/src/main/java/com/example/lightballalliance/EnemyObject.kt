package com.example.lightballalliance

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

// number of coordinates per vertex in this array
const val COORDS_PER_VERTEX = 3
class EnemyObject {
  private var cubeCoords = floatArrayOf(
    // Front face
    -0.5f, -0.5f,  0.5f,
    0.5f, -0.5f,  0.5f,
    0.5f,  0.5f,  0.5f,
    -0.5f,  0.5f,  0.5f,
    // Back face
    -0.5f, -0.5f, -0.5f,
    -0.5f,  0.5f, -0.5f,
    0.5f,  0.5f, -0.5f,
    0.5f, -0.5f, -0.5f,
    // Top face
    -0.5f,  0.5f, -0.5f,
    -0.5f,  0.5f,  0.5f,
    0.5f,  0.5f,  0.5f,
    0.5f,  0.5f, -0.5f,
    // Bottom face
    -0.5f, -0.5f, -0.5f,
    0.5f, -0.5f, -0.5f,
    0.5f, -0.5f,  0.5f,
    -0.5f, -0.5f,  0.5f,
    // Right face
    0.5f, -0.5f, -0.5f,
    0.5f,  0.5f, -0.5f,
    0.5f,  0.5f,  0.5f,
    0.5f, -0.5f,  0.5f,
    // Left face
    -0.5f, -0.5f, -0.5f,
    -0.5f, -0.5f,  0.5f,
    -0.5f,  0.5f,  0.5f,
    -0.5f, 0.5f, -0.5f,
  )

  private val vertexShaderCode =
    // This matrix member variable provides a hook to manipulate
    // the coordinates of the objects that use this vertex shader
    "uniform mat4 uMVPMatrix;" +
    "attribute vec4 vPosition;" +
    "void main() {" +
    // the matrix must be included as a modifier of gl_Position
    // Note that the uMVPMatrix factor *must be first* in order
    // for the matrix multiplication product to be correct.
    "  gl_Position = uMVPMatrix * vPosition;" +
    "}"

  private val fragmentShaderCode =
    "precision mediump float;" +
    "uniform vec4 vColor;" +
    "void main() {" +
    "  gl_FragColor = vColor;" +
    "}"

  private var mProgram: Int
  private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

  private var positionHandle: Int = 0
  private var mColorHandle: Int = 0
  private var vPMatrixHandle: Int = 0 // Use to access and set the view transformation

  private val vertexCount: Int = cubeCoords.size / COORDS_PER_VERTEX
  private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

  init {
    val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    // create empty OpenGL ES Program
    mProgram = GLES20.glCreateProgram().also {

      // add the vertex shader to program
      GLES20.glAttachShader(it, vertexShader)

      // add the fragment shader to program
      GLES20.glAttachShader(it, fragmentShader)

      // creates OpenGL ES program executables
      GLES20.glLinkProgram(it)
    }
  }

  fun draw(mvpMatrix: FloatArray) {
    // get handle to shape's transformation matrix
    vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

    // Add program to OpenGL ES environment
    GLES20.glUseProgram(mProgram)

    // get handle to vertex shader's vPosition member
    positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

      // Enable a handle to the triangle vertices
      GLES20.glEnableVertexAttribArray(it)

      // Prepare the triangle coordinate data
      GLES20.glVertexAttribPointer(
        it,
        COORDS_PER_VERTEX,
        GLES20.GL_FLOAT,
        false,
        vertexStride,
        vertexBuffer
      )

      // get handle to fragment shader's vColor member
      mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->

        // Set color for drawing the triangle
        val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
      }

      // Pass the projection and view transformation to the shader
      GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

      // Draw the triangle
      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

      // Disable vertex array
      GLES20.glDisableVertexAttribArray(it)
    }
  }

  // initialize vertex byte buffer for shape coordinates
  private val vertexBuffer: FloatBuffer =
    // (# of coordinate values * 4 bytes per float)
    ByteBuffer.allocateDirect(cubeCoords.size * 4).run {
      order(ByteOrder.nativeOrder())
      asFloatBuffer().apply {
        put(cubeCoords)
        position(0)
      }
    }

  // initialize byte buffer for the draw list
  private val drawListBuffer: ShortBuffer =
    // (# of coordinate values * 2 bytes per short)
    ByteBuffer.allocateDirect(drawOrder.size * 2).run {
      order(ByteOrder.nativeOrder())
      asShortBuffer().apply {
        put(drawOrder)
        position(0)
      }
    }
}
