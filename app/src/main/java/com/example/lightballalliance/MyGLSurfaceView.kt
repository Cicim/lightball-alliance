package com.example.lightballalliance

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
  private val renderer: MyGLRenderer

  init {
    // Create an OpenGL ES 2.0 context
    setEGLContextClientVersion(2)

    // Render the view only when there is a change in the drawing data
    // renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    renderer = MyGLRenderer()

    // Set the Renderer for drawing on the GLSurfaceView
    setRenderer(renderer)
  }

  // Function to set the camera position according to the orientation angles
  // of the device
  fun setCamPos(x: Double, y: Double, z: Double) {
    renderer.setCamPos(x, y, z)
  }

  // Function to add an enemy to the game
  fun addEnemy(translationVector: FloatArray) {
    renderer.addEnemy(translationVector)
  }
}

class MyGLRenderer : GLSurfaceView.Renderer {
  private val enemies = mutableListOf<EnemyObject>()

  // vPMatrix is an abbreviation for "Model View Projection Matrix"
  private val vPMatrix = FloatArray(16)
  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val translateMatrix = FloatArray(16)

  private var camX = 0f
  private var camY = 0f
  private var camZ = 0f
  private var eyeX = 0f
  private var eyeY = 0f
  private var eyeZ = 6f

  // Function to set the camera position according to the orientation angles
  // of the device, using roll, pitch, and yaw
  fun setCamPos(roll: Double, pitch: Double, yaw: Double) {
    camX = eyeX - sin(yaw).toFloat() * cos(pitch).toFloat()
    camY = eyeY + sin(pitch).toFloat()
    camZ = eyeZ - cos(yaw).toFloat() * cos(pitch).toFloat()
  }

  // Function to add an enemy to the game with a given translation vector
  fun addEnemy(translationVector: FloatArray) {
    enemies.add(EnemyObject(translationVector))
  }

  override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
    // Set the background frame color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

    // Add enemies to the game
    addEnemy(floatArrayOf(3f, 3f, 3f))
    addEnemy(floatArrayOf(2f, 2f, 2f))
    addEnemy(floatArrayOf(1f, 1f, 1f))
    addEnemy(floatArrayOf(0f, 0f, 0f))
    addEnemy(floatArrayOf(-1f, -1f, 1f))
    addEnemy(floatArrayOf(-2f, -2f, 2f))
    addEnemy(floatArrayOf(-3f, -3f, 3f))
  }

  override fun onDrawFrame(unused: GL10) {
    val scratch = FloatArray(16)

    // Redraw background color
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    // Set the camera position (View matrix)
    Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, camX, camY, camZ, 0f, 1f, 0f)

    // Calculate the projection and view transformation
    Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    enemies.forEach {
      // Get the translation vector for the enemy
      val (tx, ty, tz) = it.getTranslationVector()

      // Create a translation transformation
      Matrix.setIdentityM(translateMatrix, 0)
      Matrix.translateM(translateMatrix,0, tx, ty, tz)

      // Combine the translation matrix with the projection and camera view
      Matrix.multiplyMM(scratch, 0, vPMatrix, 0, translateMatrix, 0)

      // Draw shape
      it.draw(scratch)
    }
  }

  override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)

    val ratio: Float = width.toFloat() / height.toFloat()

    // this projection matrix is applied to object coordinates
    // in the onDrawFrame() method
    Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 7f)
  }
}

fun loadShader(type: Int, shaderCode: String): Int {
  // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
  // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
  return GLES20.glCreateShader(type).also { shader ->
    // add the source code to the shader and compile it
    GLES20.glShaderSource(shader, shaderCode)
    GLES20.glCompileShader(shader)
  }
}
