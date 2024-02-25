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
import com.example.lightballalliance.data.ClientMessage
import com.example.lightballalliance.data.Enemy
import com.example.lightballalliance.data.Game
import com.example.lightballalliance.data.GameMessage
import com.example.lightballalliance.data.sendClientMessage
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

class GameActivity : AppCompatActivity(), SensorEventListener, WebSocketListener {
  private lateinit var sensorManager: SensorManager
  private lateinit var gLView: MyGLSurfaceView
  private var game: Game? = null

  // Current orientation angles (calibrated)
  private var gameRotation = DoubleArray(3)
  // Rotation the player should have when calibrated
  private var initialGameRotation = DoubleArray(3)

  // Current orientation angles (uncalibrated)
  private var currentEuler = DoubleArray(3)
  // Current orientation quaternion (uncalibrated)
  private var currentQuat = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

  // Orientation angles when the device was calibrated.
  private var calibrationEuler = DoubleArray(3)
  // Quaternion to calibrate the sensors
  private var calibrationQuat = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

  private val isConnected = mutableStateOf(false)
  private val orientationViewModel: OrientationViewModel by viewModels()

  private var timer: Timer = Timer()
  private val timerPeriod = 20L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the variables
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    WebSocketClient.setGameListener(this@GameActivity)
    calibrationEuler = DoubleArray(3)
    isConnected.value = true

    gLView = MyGLSurfaceView(this)
    setContentView(gLView)

    // Send the player_ready message
    sendClientMessage(ClientMessage.Ready)

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
    when (e.action) {
      MotionEvent.ACTION_DOWN -> {
        val x = e.x
        val y = e.y

        // Check if the player has touched the shoot button
        if (x <= 0.6 * gLView.width && x >= 0.4 * gLView.width && y >= 0.8 * gLView.height) {
          Log.d("GameActivity", ">>>Shoot button pressed")
          game?.shoot()
        }

        // Else calibrate the sensors
        else {
          // Copy the current orientation angles to the calibration array
          calibrationEuler = currentEuler.copyOf()

          // Store the conjugate of the quaternion to calibrate the sensors
          calibrationQuat[0] = -currentQuat[0]
          calibrationQuat[1] = -currentQuat[1]
          calibrationQuat[2] = -currentQuat[2]
          calibrationQuat[3] = currentQuat[3]

          // Recompute the orientation angles
          recomputePlayerAngle()
        }
      }
    }
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
      // The sensor reading outputs:
      // - 2nd quaternion component x: sin(θ/2) * x
      // - 3rd quaternion component y: sin(θ/2) * y
      // - 4th quaternion component z: sin(θ/2) * z
      // - 1st quaternion component w: cos(θ/2)
      // - reading accuracy in radians
      val sensorReading = event.values
      // Save the current quaternion
      currentQuat[0] = sensorReading[0]
      currentQuat[1] = sensorReading[1]
      currentQuat[2] = sensorReading[2]
      currentQuat[3] = sensorReading[3]

      // The quaternion is converted to angles to be sent to the server
      val newOrientation = rotationVectorToEulerAngles(sensorReading)
      val difference = abs(newOrientation[0] - currentEuler[0]) +
        abs(newOrientation[1] - currentEuler[1]) +
        abs(newOrientation[2] - currentEuler[2])
      currentEuler = newOrientation

      // Only update everything if the orientation angles have changed
      if (difference > 0.02) {
        recomputePlayerAngle()
      }
    }
  }

  private fun recomputePlayerAngle() {
    // Calibrate the sensors. This is done by setting the initial orientation
    // as the zero point and subtracting it from the current orientation.
    val calibratedQuat = multiplyQuaternions(
      calibrationQuat,
      currentQuat
    )

    // Update the game rotation angles
    gameRotation = rotationVectorToEulerAngles(calibratedQuat)

    // Send the orientation angles to the server (yes, before recalculating the final orientation)
    // The server wants the angles only as a rotation compared to the initial orientation.
    sendData()

    // Convert the initial orientation angles to a quaternion
    val initialRotationQuat = eulerAnglesToQuaternion(
      initialGameRotation[0],
      initialGameRotation[1],
      initialGameRotation[2]
    )

    // Multiply the quaternions
    val finalOrientation = multiplyQuaternions(
      calibratedQuat,
      initialRotationQuat
    )

    val finalOrientationAngles = rotationVectorToEulerAngles(finalOrientation)

    // Redraw the GLSurfaceView
    game?.setCameraOrientation(
      finalOrientationAngles[2],
      finalOrientationAngles[0],
      cos(initialGameRotation[1]) * finalOrientationAngles[1]
    )
  }

  // Function to convert a quaternion to Euler angles (roll, pitch, and yaw)
  private fun rotationVectorToEulerAngles(v: FloatArray): DoubleArray {
    val qx = v[0].toDouble()
    val qy = v[1].toDouble()
    val qz = v[2].toDouble()
    val qw = v[3].toDouble()

    val sinr_cosp = 2 * (qw * qx + qy * qz)
    val cosr_cosp = 1 - 2 * (qx * qx + qy * qy)
    val roll = atan2(sinr_cosp, cosr_cosp)

    // pitch / y
    val sinp = 2 * (qw * qy - qz * qx)
    val pitch = if (abs(sinp) >= 1) {
      Math.PI / 2 * sign(sinp)
    } else {
      asin(sinp)
    }

    // yaw / z
    val siny_cosp = 2 * (qw * qz + qx * qy)
    val cosy_cosp = 1 - 2 * (qy * qy + qz * qz)
    val yaw = atan2(siny_cosp, cosy_cosp)

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

    // Send the orientation angles to the server
    sendClientMessage(ClientMessage.RotationUpdated(
      gameRotation[0].toFloat(),
      gameRotation[1].toFloat(),
      -gameRotation[2].toFloat()
    ))
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

          game?.setCameraEye(
            player.getPosition()[0].toFloat(),
            player.getPosition()[1].toFloat(),
            player.getPosition()[2].toFloat()
          )

          // Get the initial orientation of the player
          initialGameRotation = player.getInitialRotation().clone()
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
          floatArrayOf(message.source.x, message.source.y, message.source.z),
          floatArrayOf(message.target.x, message.target.y, message.target.z)
        )
        game?.addEnemy(enemy)
      }
      is GameMessage.EnemyRemoved -> {
        game?.removeEnemy(message.id)
      }
      is GameMessage.GameOver -> {
        Log.d("GameActivity", ">>>Game over")

        timer.cancel()

        // Finish this activity and redirect to the main activity
        WebSocketClient.disconnect()
        finish()
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
