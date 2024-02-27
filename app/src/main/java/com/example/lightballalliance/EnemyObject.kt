package com.example.lightballalliance

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

// Number of coordinates per vertex
const val COORDS_PER_VERTEX_3D = 3

class EnemyObject {
  private lateinit var sphereCoords: FloatArray
  private lateinit var sphereNormals: FloatArray

  private var vertexBuffer: FloatBuffer
  private var normalBuffer: FloatBuffer

  private val vertexShaderCode =
    """
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec4 vNormal;
    
    // Output variables will be interpolated across the triangle
    varying vec4 normal;

    void main() {
      gl_Position = uMVPMatrix * vPosition;
      normal = vNormal;
    }
    """.trimIndent()

  private val fragmentShaderCode =
    """
    precision mediump float;
    uniform vec4 vColor;
    
    // Interpolated values from the vertex shaders
    varying vec4 normal;
    
    void main() {
      // Set a light source at 4, 0, 0
      vec3 light1 = vec3(1, 0, 0);
      vec3 light2 = vec3(-1, 0, 0);
      float ambient = 0.3;
      
      // Calculate the dot product of the light and normal
      float directLight1 = dot(normal.xyz, light1);
      float directLight2 = dot(normal.xyz, light2);
      float brightness = max(directLight1, directLight2);
      // Add the ambient light
      brightness = min(brightness + ambient, 1.0);
    
      gl_FragColor = vColor * brightness;
    }
    """.trimIndent()

  private var mProgram: Int

  private var positionHandle: Int = 0
  private var normalHandle: Int = 0
  private var colorHandle: Int = 0
  private var vPMatrixHandle: Int = 0 // Use to access and set the view transformation

  private var vertexCount: Int
  private var vertexStride: Int

  init {
    generateSphereCoordinates()
    vertexCount = sphereCoords.size / COORDS_PER_VERTEX_3D
    vertexStride = COORDS_PER_VERTEX_3D * 4 // 4 bytes per vertex

    // Initialize vertex byte buffer for shape coordinates
    // (# of coordinate values * 4 bytes per float)
    vertexBuffer = ByteBuffer.allocateDirect(sphereCoords.size * 4).run {
      order(ByteOrder.nativeOrder())
      asFloatBuffer().apply {
        put(sphereCoords)
        position(0)
      }
    }
    normalBuffer = ByteBuffer.allocateDirect(sphereNormals.size * 4).run {
      order(ByteOrder.nativeOrder())
      asFloatBuffer().apply {
        put(sphereNormals)
        position(0)
      }
    }

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

  private fun generateSphereCoordinates() {
    val radius = 0.1f
    val slices = 48
    val stacks = 48
    val lengthInv = 1.0f / radius
    val pi = Math.PI.toFloat()

    // Coordinates and normals for the sphere
    val coordinates = FloatArray(3 * (slices + 1) * (stacks + 1))
    val normals = FloatArray(3 * (slices + 1) * (stacks + 1))
    // Coordinate index in the two matrices
    var i = 0

    for (stack in 0..stacks) {
      val stackAngle = pi / 2 - stack * pi / stacks
      val xy = radius * cos(stackAngle)
      val z = radius * sin(stackAngle)

      for (slice in 0..slices) {
        val sliceAngle = 2 * pi / slices * slice
        val x = xy * cos(sliceAngle);
        val y = xy * sin(sliceAngle);

        // normalized vertex normal (nx, ny, nz)
        val nx = x * lengthInv;
        val ny = y * lengthInv;
        val nz = z * lengthInv;

        normals[i] = nx;
        coordinates[i++] = x
        normals[i] = ny;
        coordinates[i++] = y
        normals[i] = nz;
        coordinates[i++] = z
      }
    }

    sphereCoords = coordinates
    sphereNormals = normals
  }

  fun draw(mvpMatrix: FloatArray, color: FloatArray) {
    // Add program to OpenGL ES environment
    GLES20.glUseProgram(mProgram)

    // Get handle to vertex shader's vPosition member
    positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(positionHandle)
    // Prepare the triangle coordinate data
    GLES20.glVertexAttribPointer(
      positionHandle,
      COORDS_PER_VERTEX_3D,
      GLES20.GL_FLOAT,
      false,
      vertexStride,
      vertexBuffer
    )

    // Get handle to vertex shader's vNormal member
    normalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal")
    // Enable a handle to the triangle normals
    GLES20.glEnableVertexAttribArray(normalHandle)
    // Prepare the triangle normal data
    GLES20.glVertexAttribPointer(
      normalHandle,
      COORDS_PER_VERTEX_3D,
      GLES20.GL_FLOAT,
      false,
      vertexStride,
      normalBuffer
    )

    // Get handle to fragment shader's vColor member
    colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
    // Set color for drawing the object
    GLES20.glUniform4fv(colorHandle, 1, color, 0);

    // Get handle to shape's transformation matrix
    vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
    // Pass the projection and view transformation to the shader
    GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

    // Draw the triangle
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(positionHandle)
    // Disable normal array
    GLES20.glDisableVertexAttribArray(normalHandle)
  }

}
