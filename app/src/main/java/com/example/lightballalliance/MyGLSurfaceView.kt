package com.example.lightballalliance

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
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

  fun setCamPos(x: Double, y: Double, z: Double) {
    renderer.setCamPos(x, y, z)
  }
}

class MyGLRenderer : GLSurfaceView.Renderer {
  private lateinit var mEnemy: EnemyObject

  // vPMatrix is an abbreviation for "Model View Projection Matrix"
  private val vPMatrix = FloatArray(16)
  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
//  private val rotationMatrix = FloatArray(16)
  private val translateMatrix = FloatArray(16)

  private var camX = 0f
  private var camY = 0f
  private var camZ = 0f
  private var eyeX = 0f
  private var eyeY = 0f
  private var eyeZ = 6f

  fun setCamPos(roll: Double, pitch: Double, yaw: Double) {
    Log.d("MyGLRenderer", "roll: %.1f, pitch: %.1f, yaw: %.1f".format(roll, pitch, yaw))

    camX = eyeX - sin(yaw).toFloat() * cos(pitch).toFloat()
    camY = eyeY + sin(pitch).toFloat()
    camZ = eyeZ - cos(yaw).toFloat() * cos(pitch).toFloat()
  }

  override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
    // Set the background frame color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

    mEnemy = EnemyObject()
  }

  override fun onDrawFrame(unused: GL10) {
    val scratch = FloatArray(16)

    // Redraw background color
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    // Set the camera position (View matrix)
    Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, camX, camY, camZ, 0f, 1f, 0f)

    // Calculate the projection and view transformation
    Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    // Create a translation transformation
    Matrix.setIdentityM(translateMatrix, 0)
    Matrix.translateM(translateMatrix, 0, 0f, 0f, 0f)

    // Combine the translation matrix with the projection and camera view
    Matrix.multiplyMM(scratch, 0, vPMatrix, 0, translateMatrix, 0)

    // Create a rotation transformation
//    Matrix.setRotateM(rotationMatrix, 0, angle, 1f, 1f, 1f)

    // Combine the rotation matrix with the projection and camera view
//    Matrix.multiplyMM(scratch, 0, scratch, 0, rotationMatrix, 0)

    // Draw shape
    mEnemy.draw(scratch)
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
