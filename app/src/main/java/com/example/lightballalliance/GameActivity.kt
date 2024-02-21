package com.example.lightballalliance

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.example.lightballalliance.data.Enemy
import com.example.lightballalliance.data.Game
import com.example.lightballalliance.data.GameMessage
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameActivity : AppCompatActivity(), SensorEventListener, WebSocketListener {
  private lateinit var sensorManager: SensorManager
  private lateinit var gLView: MyGLSurfaceView
  private var game: Game? = null

  private val gameRotation = DoubleArray(3)
  private val initialGameRotation = mutableStateOf(DoubleArray(3))
  private val eulerAngles = mutableStateOf(DoubleArray(3))
  private val sensorsCalibration = mutableStateOf(DoubleArray(3))
  private val isConnected = mutableStateOf(false)
  private val orientationViewModel: OrientationViewModel by viewModels()

  private var timer: Timer = Timer()
  private val timerPeriod = 20L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the variables
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    WebSocketClient.setGameListener(this@GameActivity)
    sensorsCalibration.value = DoubleArray(3)
    isConnected.value = true

    gLView = MyGLSurfaceView(this)
    setContentView(gLView)

    // Send the player_ready message
    WebSocketClient.send("""{"type": "player_ready", "data": ""}""")

    // Start the timer to update the game state
    timer = timer("updateGameStateTimer",
      false,
      0,
      timerPeriod
    ) {
      game?.increaseTime(timerPeriod.toInt())
    }
  }

  override fun onTouchEvent(e: MotionEvent): Boolean {
    // Calibrate the sensors
    sensorsCalibration.value = eulerAngles.value.copyOf()
    return true
  }


  /**
   * SENSOR MANAGER
   * Retrieve the data from the accelerometer and magnetometer sensors
   * and continuously update the UI with the orientation angles.
   */

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    // Do something here if sensor accuracy changes.
    // You must implement this callback in your code.
  }

  override fun onResume() {
    super.onResume()

    sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.also { gameRotationVector ->
      sensorManager.registerListener(
        this,
        gameRotationVector,
        SensorManager.SENSOR_DELAY_NORMAL,
        SensorManager.SENSOR_DELAY_UI
      )
    }
  }

  override fun onPause() {
    super.onPause()

    // Don't receive any more updates from either sensor.
    sensorManager.unregisterListener(this)
  }

  // Get readings from accelerometer and magnetometer. To simplify calculations,
  // consider storing these readings as unit vectors.
  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
      val inputQuaternion = event.values
      eulerAngles.value = rotationVectorToEulerAngles(inputQuaternion)

      if (!eulerAngles.value.contentEquals(gameRotation)) {
        gameRotation[0] = eulerAngles.value[0] - sensorsCalibration.value[0] // roll (z)
        gameRotation[1] = eulerAngles.value[1] - sensorsCalibration.value[1] // pitch (x)
        gameRotation[2] = eulerAngles.value[2] - sensorsCalibration.value[2] // yaw (y)

        // Update the UI with the new orientation angles
        orientationViewModel.updateOrientation(
          gameRotation[0],
          gameRotation[1],
          gameRotation[2]
        )

        // Send the orientation angles to the server.
        sendData()

        // Convert the Euler angles to a quaternion
        val calibratedSensorAngles = eulerAnglesToQuaternion(
          gameRotation[0],
          gameRotation[1],
          gameRotation[2]
        )

        // Convert the initial orientation angles to a quaternion
        val initialRotationAngles = eulerAnglesToQuaternion(
          initialGameRotation.value[0],
          initialGameRotation.value[1],
          initialGameRotation.value[2]
        )

        // Multiply the quaternions
        val finalOrientation = multiplyQuaternions(
          initialRotationAngles,
          calibratedSensorAngles
        )

        // Convert the final orientation to Euler angles
//        finalOrientation[0] = finalOrientation[0] * finalOrientation[3]
//        finalOrientation[1] = finalOrientation[1] * finalOrientation[3]
//        finalOrientation[2] = finalOrientation[2] * finalOrientation[3]
        val finalOrientationAngles = rotationVectorToEulerAngles(finalOrientation)

        // Redraw the GLSurfaceView
        gLView.setCamOrientation(
          finalOrientationAngles[1],
          finalOrientationAngles[0],
          finalOrientationAngles[2]
        )
      }
    }
  }

  // Function to convert a quaternion to Euler angles (roll, pitch, and yaw)
  private fun rotationVectorToEulerAngles(v: FloatArray): DoubleArray {
    val qx = v[0].toDouble() // x * sin(theta / 2)
    val qy = v[1].toDouble() // y * sin(theta / 2)
    val qz = v[2].toDouble() // z * sin(theta / 2)

    val norm = sqrt(qx * qx + qy * qy + qz * qz)
    if (norm == 0.0) {
      return doubleArrayOf(0.0, 0.0, 0.0)
    }

    // cos(theta / 2)
    val qw = sqrt(1.0 - norm * norm)

    val sinRcosP = 2 * (qw * qx + qy * qz)
    val cosRcosP = 1 - 2 * (qx * qx + qy * qy)
    val roll = ceil(atan2(sinRcosP, cosRcosP) * 1000) / 1000

    val sinP = sqrt(1 + 2 * (qw * qy - qx * qz))
    val cosP = sqrt(1 - 2 * (qw * qy - qx * qz))
    val pitch = ceil((atan2(sinP, cosP) - Math.PI / 2) * 1000) / 1000

    val sinYcosP = 2 * (qw * qz + qx * qy)
    val cosYcosP = 1 - 2 * (qy * qy + qz * qz)
    val yaw = ceil(atan2(sinYcosP, cosYcosP) * 1000) / 1000

    return doubleArrayOf(roll, pitch, yaw)
  }

  // Function to convert Euler angles to a quaternion
  private fun eulerAnglesToQuaternion(roll: Double, pitch: Double, yaw: Double): FloatArray {
    val cy = cos(yaw * 0.5).toFloat()
    val sy = sin(yaw * 0.5).toFloat()
    val cp = cos(pitch * 0.5).toFloat()
    val sp = sin(pitch * 0.5).toFloat()
    val cr = cos(roll * 0.5).toFloat()
    val sr = sin(roll * 0.5).toFloat()

    val w = cr * cp * cy + sr * sp * sy
    val x = sr * cp * cy - cr * sp * sy
    val y = cr * sp * cy + sr * cp * sy
    val z = cr * cp * sy - sr * sp * cy

    return floatArrayOf(x, y, z, w)
  }

  // Function to multiply two quaternions
  private fun multiplyQuaternions(q1: FloatArray, q2: FloatArray): FloatArray {
    val x = q1[3] * q2[0] + q1[0] * q2[3] + q1[1] * q2[2] - q1[2] * q2[1]
    val y = q1[3] * q2[1] - q1[0] * q2[2] + q1[1] * q2[3] + q1[2] * q2[0]
    val z = q1[3] * q2[2] + q1[0] * q2[1] - q1[1] * q2[0] + q1[2] * q2[3]
    val w = q1[3] * q2[3] - q1[0] * q2[0] - q1[1] * q2[1] - q1[2] * q2[2]

    return floatArrayOf(x, y, z, w)
  }

  private fun sendData() {
    if (!isConnected.value || game == null) {
      return
    }

    // Send the orientation angles to the server.
    val x = "%.3f".format(gameRotation[0]).replace(",", ".")
    val z = "%.3f".format(-gameRotation[1]).replace(",", ".")
    val y = "%.3f".format(gameRotation[2]).replace(",", ".")

    val message = """{"type": "player_rotation_updated", "data": {"x": $x, "y": $y, "z": $z}}"""

    WebSocketClient.send(message)
  }


  /**
   * WEBSOCKET CLIENT
   * Connect to the server and send the orientation angles.
   */

  override fun onConnected() {
    // Nothing to do here
  }

  override fun onMessage(message: GameMessage) {
//    Log.d("GameActivity", ">Received: $message")

    when (message) {
      is GameMessage.GameStarted -> {
        // Start the game
        Log.d("GameActivity", ">>>Game started")

        // Instantiate a new Game object
        game = Game(message.players)
        gLView.setGameHandler(game!!)

        // Set the initial position of the camera
        val player = game?.getPlayer(WebSocketClient.playerName.value)
        if (player != null) {
          Log.d("PlayerPos", ">>>Initial position: ${player.getPosition().contentToString()}")

          gLView.setInitialEye(
            player.getPosition()[0].toFloat(),
            player.getPosition()[1].toFloat(),
            player.getPosition()[2].toFloat()
          )

          // Get the initial orientation of the player
          initialGameRotation.value = player.getInitialRotation().clone()
        }
      }
      is GameMessage.TimeSync -> {
        // Synchronize the time with the server
        game?.syncTime(message.time)
      }
      is GameMessage.EnemyAdded -> {
        val enemy = Enemy(
          message.id,
          message.health,
          message.color,
          message.startTime,
          message.speed,
          doubleArrayOf(message.source.x, message.source.y, message.source.z),
          doubleArrayOf(message.target.x, message.target.y, message.target.z)
        )
        game?.addEnemy(enemy)
      }
      is GameMessage.EnemyRemoved -> {
        game?.removeEnemy(message.id)
      }
      is GameMessage.GameOver -> {
        Log.d("GameActivity", ">>>Game over")

        timer.cancel()

        // Go back to the main activity stopping the current activity
//        intent = Intent(this, MainActivity::class.java)
//        startActivity(intent)
      }

      else -> { }
    }
  }

  override fun onDisconnected() {
    // Redirect to the main activity
    Log.d("GameActivity", ">>>Disconnected")
    isConnected.value = false

    intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
  }
}
