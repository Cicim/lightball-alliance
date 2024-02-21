package com.example.lightballalliance

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.example.lightballalliance.data.Game
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

  fun setGameHandler(game: Game) {
    renderer.setGameHandler(game)
  }

  // Function to set the camera orientation according to the orientation angles
  // of the device
  fun setCamOrientation(x: Double, y: Double, z: Double) {
    renderer.setCamOrientation(x, y, z)
  }

  // Function to set the initial position of the camera (eye)
  fun setInitialEye(x: Float, y: Float, z: Float) {
    renderer.setInitialEye(x, y, z)
  }
}

class MyGLRenderer : GLSurfaceView.Renderer {
  private lateinit var enemyObject: EnemyObject
  private var game: Game? = null

  // vPMatrix is an abbreviation for "Model View Projection Matrix"
  private val vPMatrix = FloatArray(16)
  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val translateMatrix = FloatArray(16)

  // Variables to set the camera position
  private var centerX = 0f
  private var centerY = 0f
  private var centerZ = 0f
  private var eyeX = 0f
  private var eyeY = 0f
  private var eyeZ = 6f

  // Function to set the camera orientation according to the orientation angles
  // of the device, using roll, pitch, and yaw
  fun setCamOrientation(roll: Double, pitch: Double, yaw: Double) {
    centerX = eyeX - sin(yaw).toFloat() * cos(pitch).toFloat()
    centerY = eyeY + sin(pitch).toFloat()
    centerZ = eyeZ - cos(yaw).toFloat() * cos(pitch).toFloat()
  }

  // Function to set the initial position of the camera (eye)
  fun setInitialEye(x: Float, y: Float, z: Float) {
    eyeX = x
    eyeY = y
    eyeZ = z
  }

  // Function to set the handler for the game
  fun setGameHandler(game: Game) {
    this.game = game
  }

  override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
    // Set the background frame color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

    // Initialize the enemy object
    enemyObject = EnemyObject()
  }

  override fun onDrawFrame(unused: GL10) {
    val scratch = FloatArray(16)

    // Redraw background color
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    // Set the camera position (View matrix)
    Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, 0f, 1f, 0f)

    // Calculate the projection and view transformation
    Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    if (game == null) {
      return
    }

    try {
      game!!.getEnemies().forEach {
        // Get the translation vector for the enemy
        val (tx, ty, tz) = it.getPosition(game!!.getTime())

        // Create a translation transformation
        Matrix.setIdentityM(translateMatrix, 0)
        Matrix.translateM(translateMatrix,0, tx.toFloat(), ty.toFloat(), tz.toFloat())

        // Combine the translation matrix with the projection and camera view
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, translateMatrix, 0)

        // Draw shape
        enemyObject.draw(scratch, it.getColor())
      }
    } catch (e: Exception) {
      Log.e("MyGLRenderer", "Error: ${e.message}")
    }
  }

  override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)

    val ratio: Float = width.toFloat() / height.toFloat()

    // this projection matrix is applied to object coordinates
    // in the onDrawFrame() method
    Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 12f)
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
