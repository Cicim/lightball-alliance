package com.example.lightballalliance

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


class AllyObject {
  val H = 1f
  val W = 0.5f
  val D = 0.5f

  private var cubeCoordinates = floatArrayOf(
    // Front face -- Starting from the the top left closest corner
    -W, H, D,
    W, H, D,
    W, -H, D,
    -W, -H, D,
    // Back face
    -W, H, -D,
    -W, -H, -D,
    W, -H, -D,
    W, H, -D,
    // Top face
    -W, H, -D,
    -W, H, D,
    W, H, D,
    W, H, -D,
    // Bottom face
    -W, -H, -D,
    W, -H, -D,
    W, -H, D,
    -W, -H, D,
    // Right face
    W, -H, -D,
    W, H, -D,
    W, H, D,
    W, -H, D,
    // Left face
    -W, -H, -D,
    -W, -H, D,
    -W, H, D,
    -W, H, -D,
  )

  private val cubeNormals = floatArrayOf(
    // Front face
    0f, 0f, 1f,
    0f, 0f, 1f,
    0f, 0f, 1f,
    0f, 0f, 1f,
    // Back face
    0f, 0f, -1f,
    0f, 0f, -1f,
    0f, 0f, -1f,
    0f, 0f, -1f,
    // Top face
    0f, 1f, 0f,
    0f, 1f, 0f,
    0f, 1f, 0f,
    0f, 1f, 0f,
    // Bottom face
    0f, -1f, 0f,
    0f, -1f, 0f,
    0f, -1f, 0f,
    0f, -1f, 0f,
    // Right face
    1f, 0f, 0f,
    1f, 0f, 0f,
    1f, 0f, 0f,
    1f, 0f, 0f,
    // Left face
    -1f, 0f, 0f,
    -1f, 0f, 0f,
    -1f, 0f, 0f,
    -1f, 0f, 0f,
  )

  private val vertexShaderCode =
    """
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec4 vNormal;
    
    varying vec4 normal;
    
    void main() {
      gl_Position = uMVPMatrix * vPosition;
      normal = vNormal;
    }
    """.trimIndent()

  private val fragmentShaderCode =
    """
    precision mediump float;
    varying vec4 normal;

    void main() {
      vec3 baseColor = vec3(0.1, 0.4, 1);
      vec3 lightDirection = normalize(vec3(1, 1, 1));
      float lightIntensity = max(dot(normalize(normal.xyz), lightDirection), 0.0);
      lightIntensity = 0.3 + lightIntensity * 0.7;
      
      gl_FragColor = vec4(baseColor * lightIntensity, 1.0);
    }
    """.trimIndent()

  private var mProgram: Int

  private var positionHandle: Int = 0
  private var normalHandle: Int = 0
  private var vPMatrixHandle: Int = 0 // Use to access and set the view transformation

  private val vertexCount: Int = cubeCoordinates.size / COORDS_PER_VERTEX_3D
  private val vertexStride: Int = COORDS_PER_VERTEX_3D * 4 // 4 bytes per vertex

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

    // Get handle to vertex shader's vPosition member
    positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(positionHandle)

    // Prepare the object's coordinate data
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

    // Prepare the object's normal data
    GLES20.glVertexAttribPointer(
      normalHandle,
      COORDS_PER_VERTEX_3D,
      GLES20.GL_FLOAT,
      false,
      vertexStride,
      normalBuffer
    )

    // Pass the projection and view transformation to the shader
    GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

    // Draw the triangles
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

    // Disable vertex arrays
    GLES20.glDisableVertexAttribArray(normalHandle)
    GLES20.glDisableVertexAttribArray(positionHandle)
  }


  // initialize vertex byte buffer for shape coordinates
  private val vertexBuffer: FloatBuffer =
    // (# of coordinate values * 4 bytes per float)
    ByteBuffer.allocateDirect(cubeCoordinates.size * 4).run {
      order(ByteOrder.nativeOrder())
      asFloatBuffer().apply {
        put(cubeCoordinates)
        position(0)
      }
    }

  private val normalBuffer: FloatBuffer =
    // (# of coordinate values * 4 bytes per float)
    ByteBuffer.allocateDirect(cubeNormals.size * 4).run {
      order(ByteOrder.nativeOrder())
      asFloatBuffer().apply {
        put(cubeNormals)
        position(0)
      }
    }
}
