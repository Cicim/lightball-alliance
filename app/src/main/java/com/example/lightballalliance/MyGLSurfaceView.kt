package com.example.lightballalliance

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.example.lightballalliance.data.Game
import com.example.lightballalliance.data.GameOverReason
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

const val CAMERA_INTERPOLATION_TIME = 10
val YOUR_TEXT_COLOR = floatArrayOf(0.75f, 0.75f, 1f, 1f)
val ALLY_TEXT_COLOR = floatArrayOf(0.75f, 1f, 0.75f, 1f)

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
  private val renderer: MyGLRenderer

  init {
    // Create an OpenGL ES 2.0 context
    setEGLContextClientVersion(2)

    // Render the view only when there is a change in the drawing data
    // renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    renderer = MyGLRenderer(context)

    // Set the Renderer for drawing on the GLSurfaceView
    setRenderer(renderer)
  }

  fun setGameHandler(game: Game) {
    renderer.setGameHandler(game)
  }

  fun setPlayerReady() {
    renderer.setPlayerReady()
  }

  fun setGameMatched() {
    renderer.setGameMatched()
  }
}

class MyGLRenderer (private val context: Context) : GLSurfaceView.Renderer {
  // Rendered objects
  private lateinit var enemyObject: EnemyObject
  private lateinit var allyObject: AllyObject

  // HUD objects
  private lateinit var shootButton: TexturedSquareObject
  private lateinit var gunSight: TexturedSquareObject
  private lateinit var calibrateButton: TexturedSquareObject
  private lateinit var playerHealthBar: HealthBarObject
  private lateinit var allyHealthBar: HealthBarObject

  // Before game starts
  private lateinit var readyButton: TexturedSquareObject
  private lateinit var readyText: TexturedSquareObject
  private lateinit var waitingForMatchText: TexturedSquareObject
  private lateinit var waitingForAllyText: TexturedSquareObject
  private var isPlayerReady: Boolean = false
  private var isGameMatched: Boolean = false

  // End game objects
  private lateinit var wonText: TexturedSquareObject
  private lateinit var lostText: TexturedSquareObject
  private lateinit var tiedText: TexturedSquareObject
  private lateinit var youDiedText: TexturedSquareObject
  private lateinit var allyDiedText: TexturedSquareObject
  private lateinit var allyDisconnectedText: TexturedSquareObject
  private lateinit var mainPageText: TexturedSquareObject

  // Numbers and texts
  private lateinit var youText: TexturedSquareObject
  private lateinit var allyText: TexturedSquareObject
  private lateinit var pointsText: TexturedSquareObject
  private lateinit var digits: Array<TexturedSquareObject>

  private var game: Game? = null

  // First element of the queue of two camera orientation quaternions
  private var sourceQuaternion: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
  // Second element of the queue of two camera orientation quaternions
  private var targetQuaternion: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
  // Orientation interpolation timer
  private var orientationTimer: Int = 0

  // vPMatrix is an abbreviation for "Model View Projection Matrix"
  private val vPMatrix = FloatArray(16)
  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)

  /**
   * Setters
   */

  fun setGameHandler(game: Game) {
    this.game = game
  }

  fun setPlayerReady() {
    isPlayerReady = true
  }

  fun setGameMatched() {
    isGameMatched = true
  }

  /**
   * OpenGL ES 2.0 functions
   */

  override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
    // Set the background frame color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    // Enable depth test
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    // Enable transparency
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    // Retrieve the aspect ratio of the device
    val displayMetrics = context.resources.displayMetrics
    val aspectRatio: Float = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat()

    // Initialize the enemy object
    enemyObject = EnemyObject()
    // Initialize the ally object
    allyObject = AllyObject()

    /**
     * Initialize the HUD objects
     */
    shootButton = TexturedSquareObject(context, aspectRatio, "shootButton.png")
    gunSight = TexturedSquareObject(context, aspectRatio,"gunSight_wh.png", 0.1f, 0f, 0f)
    calibrateButton = TexturedSquareObject(context, aspectRatio, "calibrateButton.png", 0.1f, 0.43f, 0.93f)
    // Initialize the health bar objects (original aspect ratio of the image is 9:1)
    playerHealthBar = HealthBarObject(context, aspectRatio / 9f, "healthBar.png", 0.03f, -0.035f, -0.95f)
    allyHealthBar = HealthBarObject(context, aspectRatio / 9f, "healthBar.png", 0.03f, 0.035f, -0.95f)

    /**
     * Initialize the objects before the game starts
     */
    readyButton = TexturedSquareObject(context, aspectRatio, "readyButton.png", 0.5f, 0f, 0f)
    // The aspect ratio of the ready text is 2.83:1
    readyText = TexturedSquareObject(context, aspectRatio / 2.83f, "readyText.png", 0.3f, 0f, 0.6f)
    // The aspect ratio of the waiting for match text is 11.67:1
    waitingForMatchText = TexturedSquareObject(context, aspectRatio / 11.67f, "waitingForMatchText.png", 0.07f, 0f, -0.5f)
    // The aspect ratio of the waiting for ally text is 5.3:1
    waitingForAllyText = TexturedSquareObject(context, aspectRatio / 5.3f, "waitingForAllyText.png", 0.15f, 0f, -0.5f)

    /**
     * Initialize the end game objects
     */
    // Original aspect ratio of the image is 2.95:1
    wonText = TexturedSquareObject(context, aspectRatio / 2.95f, "wonText.png", 0.3f, 0f, 0.2f)
    // Original aspect ratio of the image is 2.95:1
    lostText = TexturedSquareObject(context, aspectRatio / 2.95f, "lostText.png", 0.3f, 0f, 0.2f)
    // Original aspect ratio of the image is 2.95:1
    tiedText = TexturedSquareObject(context, aspectRatio / 2.95f, "tiedText.png", 0.3f, 0f, 0.2f)
    // Original aspect ratio of the image is 3.79:1
    youDiedText = TexturedSquareObject(context, aspectRatio / 3.79f, "youDiedText.png", 0.22f, 0f, 0.2f)
    // Original aspect ratio of the image is 4.05:1
    allyDiedText = TexturedSquareObject(context, aspectRatio / 4.05f, "allyDiedText.png", 0.22f, 0f, 0.2f)
    // Original aspect ratio of the image is 3.6:1
    allyDisconnectedText = TexturedSquareObject(context, aspectRatio / 3.6f, "allyDisconnectedText.png", 0.22f, 0f, 0.2f)
    // Original aspect ratio of the image is 10.69:1
    mainPageText = TexturedSquareObject(context, aspectRatio / 10.69f, "mainPageText.png", 0.08f, 0f, -0.4f)

    /**
     * Initialize the numbers and texts
     */
    // Original aspect ratio of the image is 2.85:1
    youText = TexturedSquareObject(context, aspectRatio / 2.85f, "youText.png", 0.05f, -0.1f, -0.1f)
    // Original aspect ratio of the image is 2.69:1
    allyText = TexturedSquareObject(context, aspectRatio / 2.69f, "allyText.png", 0.06f, -0.1f, -0.2f)
    // Original aspect ratio of the image is 3.34:1
    pointsText = TexturedSquareObject(context, aspectRatio / 3.34f, "pointsText.png", 0.07f, 0.06f, -0.1f)
    // Original aspect ratio of the images is 0.66:1
    digits = Array(10) { i ->
      TexturedSquareObject(context, aspectRatio / 0.66f, "digit$i.png", 0.06f, 0f, -0.1f)
    }
  }

  override fun onDrawFrame(unused: GL10) {
    // Redraw background color
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    // If the game is not started yet, draw the ready screen
    if (this.game == null) return drawReadyScreen()

    val game = this.game!!

    // If the game is over, draw the end game interface
    if (game.isGameOver()) return drawGameOverScreen()

    // Otherwise draw the game interface
    cameraTurningLogic()

    val eye = game.getCameraEye()
    val center = game.getCameraCenter()

    // Set the camera position (View matrix)
    Matrix.setLookAtM(
      viewMatrix, 0,
      eye[0], eye[1], eye[2],
      center[0], center[1], center[2],
      0f, 1f, 0f
    )

    // Calculate the projection and view transformation
    Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    // Draw stuff with a variable orientation and position
    drawEnemies()
    drawAlly()

    // Clear the depth buffer
    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

    /**
     * Draw the HUD objects
     */
    drawShootButton()
    gunSight.draw()
    calibrateButton.draw()
    playerHealthBar.draw(game.getYourPlayer().getHealth())
    allyHealthBar.draw(game.getAllyPlayer().getHealth())
    drawScores()
  }

  override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)

    val ratio: Float = width.toFloat() / height.toFloat()

    // this projection matrix is applied to object coordinates
    // in the onDrawFrame() method
    Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 12f)
  }


  /**
   * Screen drawing helpers
   */

  private fun drawReadyScreen() {
    // Draw the ready button and its text
    readyText.draw()
    readyButton.draw(when (isPlayerReady) {
      true -> floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)
      else -> floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    })

    if (isGameMatched) {
      if (isPlayerReady)
        waitingForAllyText.draw()
    }
    else
      waitingForMatchText.draw()
  }

  private fun drawScores() {
    val game = this.game ?: return

    youText.setParams(-0.14f, -0.9f, 0.04f)
    youText.draw(YOUR_TEXT_COLOR)

    allyText.setParams(0.09f, -0.9f, 0.05f)
    allyText.draw(ALLY_TEXT_COLOR)

    val yourDigits = calculateDigits(game.getYourPlayer().getScore())
    var emptyDigits = 3 - yourDigits.size
    for (i in yourDigits.indices) {
      digits[yourDigits[i]].setParams(-0.4f + i * 0.05f + emptyDigits * 0.05f, -0.9f, 0.04f)
      digits[yourDigits[i]].draw(YOUR_TEXT_COLOR)
    }

    val allyDigits = calculateDigits(game.getAllyPlayer().getScore())
    emptyDigits = 3 - allyDigits.size
    for (i in allyDigits.indices) {
      digits[allyDigits[i]].setParams(0.55f + i * 0.05f + emptyDigits * 0.05f, -0.9f, 0.04f)
      digits[allyDigits[i]].draw(ALLY_TEXT_COLOR)
    }
  }

  private fun drawShootButton() {
    val game = this.game ?: return

    // Update the shooting timeout
    if (game.shootTimer > 0) {
      game.shootTimer -= 1
    } else {
      game.lastShootResult = null
    }

    // Compute the animation of the shoot button
    var t = game.shootTimer.toFloat() / 120f
    t = sqrt(t / 2)

    // Draw the shoot button
    shootButton.draw(when (game.lastShootResult) {
      true -> floatArrayOf(1.0f - t, 1.0f, 1.0f - t, 1.0f)
      false -> floatArrayOf(1.0f, 1.0f - t, 1.0f - t, 1.0f)
      else -> floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    })
  }

  private fun drawGameOverScreen() {
    val game = this.game ?: return

    // Draw the end game reason
    when (val reason = game.getGameOverReason()!!) {
      is GameOverReason.Won -> {

        if (reason.username == game.getYourPlayer().getUsername()) wonText.draw()
        else lostText.draw()
      }
      is GameOverReason.Tied -> {

        tiedText.draw()
      }
      is GameOverReason.Died -> {
        if (reason.username == game.getYourPlayer().getUsername()) youDiedText.draw()
        else allyDiedText.draw()
      }
      is GameOverReason.Disconnect -> allyDisconnectedText.draw()
    }

    // Draw the scores
    youText.setParams(-0.1f, -0.1f, 0.05f)
    youText.draw(YOUR_TEXT_COLOR)
    allyText.setParams(-0.1f, -0.2f, 0.06f)
    allyText.draw(ALLY_TEXT_COLOR)

    pointsText.setParams(0.06f, -0.1f)
    pointsText.draw(YOUR_TEXT_COLOR)
    pointsText.setParams(0.06f, -0.2f)
    pointsText.draw(ALLY_TEXT_COLOR)

    val yourDigits = calculateDigits(game.getYourPlayer().getScore())
    var emptyDigits = 3 - yourDigits.size
    for (i in yourDigits.indices) {
      digits[yourDigits[i]].setParams(-0.15f + i * 0.08f + emptyDigits * 0.08f, -0.1f, 0.06f)
      digits[yourDigits[i]].draw(YOUR_TEXT_COLOR)
    }

    val allyDigits = calculateDigits(game.getAllyPlayer().getScore())
    emptyDigits = 3 - allyDigits.size
    for (i in allyDigits.indices) {
      digits[allyDigits[i]].setParams(-0.15f + i * 0.08f + emptyDigits * 0.08f, -0.2f, 0.06f)
      digits[allyDigits[i]].draw(ALLY_TEXT_COLOR)
    }

    // Draw the text (button) to go back to the main page
    mainPageText.draw()
  }

  /**
   * Game object drawing helpers
   */

  private fun drawEnemies() {
    val game = this.game ?: return

    val translateMatrix = FloatArray(16)
    val positionMatrix = FloatArray(16)

    // Draw the enemies
    try {
      game.getEnemies().forEach {
        // Get the translation vector for the enemy
        val (tx, ty, tz) = it.getPosition(game.getTime())

        // Create a translation transformation
        Matrix.setIdentityM(translateMatrix, 0)
        Matrix.translateM(translateMatrix, 0, tx, ty, tz)

        // Combine the translation matrix with the projection and camera view
        Matrix.multiplyMM(positionMatrix, 0, vPMatrix, 0, translateMatrix, 0)

        // Draw shape
        enemyObject.draw(positionMatrix, it.getColor())
      }
    } catch (e: Exception) {
      Log.e("MyGLRenderer", "Error: ${e.message}")
    }
  }

  private fun drawAlly() {
    val game = this.game ?: return

    val translationM = FloatArray(16)
    val rotationM = FloatArray(16)
    val resultM = FloatArray(16)

    // Get the translation vector for the ally
    val (tx, ty, tz) = game.getAllyPlayer().getPosition()

    // Create a translation transformation
    Matrix.setIdentityM(translationM, 0)
    Matrix.translateM(translationM, 0, tx, ty, tz)

    // Create a rotation transformation
    val (axis, angle) = game.getAllyRotation()
    Matrix.setIdentityM(rotationM, 0)
    val a = angle / Math.PI.toFloat() * 180
    Matrix.rotateM(rotationM, 0, a, axis[0], axis[1], axis[2])

    // Combine the translation and rotation matrices with the projection and camera view
    Matrix.multiplyMM(resultM, 0, translationM, 0, rotationM, 0)
    Matrix.multiplyMM(resultM, 0, vPMatrix, 0, resultM, 0)

    // Draw shape
    allyObject.draw(resultM)
  }

  /**
   * Helper functions
   */

  // Function to calculate the digits of a number
  private fun calculateDigits(score: Int): List<Int> {
    val digits = mutableListOf<Int>()
    var n = score

    if (n == 0) return listOf(0)

    while (n > 0) {
      digits.add(n % 10)
      n /= 10
    }

    return digits.reversed()
  }

  private fun cameraTurningLogic() {
    val game = this.game ?: return

    if (orientationTimer == 0) {
      // End the previous movement on the last angle
      game.setCameraOrientation(targetQuaternion)

      // Update the orientation quaternions
      sourceQuaternion = targetQuaternion
      targetQuaternion = game.getNewOrientationQuaternion()
      orientationTimer = CAMERA_INTERPOLATION_TIME
    } else {
      orientationTimer -= 1
      val t = 1.0f - orientationTimer.toFloat() / CAMERA_INTERPOLATION_TIME.toFloat()
      val orientation = slerpQuaternions(sourceQuaternion, targetQuaternion, t)

      game.setCameraOrientation(orientation)

      // If a new orientation arrived during this time, update the target quaternion
      val newTargetQuaternion = game.getNewOrientationQuaternion()
      if (!newTargetQuaternion.contentEquals(targetQuaternion)) {
        sourceQuaternion = orientation
        targetQuaternion = newTargetQuaternion
        orientationTimer = CAMERA_INTERPOLATION_TIME
      }
    }
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
