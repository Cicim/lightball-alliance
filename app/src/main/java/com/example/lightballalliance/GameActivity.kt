package com.example.lightballalliance

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lightballalliance.ui.theme.lightballallianceTheme
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sqrt

class GameActivity : AppCompatActivity(), SensorEventListener, WebSocketListener {
  private lateinit var sensorManager: SensorManager

  private val gameRotation = DoubleArray(3)
  private val eulerAngles = mutableStateOf(DoubleArray(3))
  private val sensorsCalibration = mutableStateOf(DoubleArray(3))
  private val isConnected = mutableStateOf(false)
  private val orientationViewModel: OrientationViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the variables
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    WebSocketClient.setGameListener(this@GameActivity)
    sensorsCalibration.value = DoubleArray(3)
    isConnected.value = true

    setContent {
      // A surface container using the 'background' color from the theme
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        lightballallianceTheme {
          SensorData(
            orientationViewModel,
            calibrate = {
              // Retrieve the current orientation angles and store them as the calibration values.
              sensorsCalibration.value = eulerAngles.value.copyOf()
            }
          )
        }
      }
    }
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
        gameRotation[0] = eulerAngles.value[0] - sensorsCalibration.value[0]
        gameRotation[1] = eulerAngles.value[1] - sensorsCalibration.value[1]
        gameRotation[2] = eulerAngles.value[2] - sensorsCalibration.value[2]

        // Update the UI with the new orientation angles
        orientationViewModel.updateOrientation(
          gameRotation[0],
          gameRotation[1],
          gameRotation[2]
        )

        // Send the orientation angles to the server.
        sendData()
      }
    }
  }

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

    val sinr_cosp = 2 * (qw * qx + qy * qz)
    val cosr_cosp = 1 - 2 * (qx * qx + qy * qy)
    val roll = ceil(atan2(sinr_cosp, cosr_cosp) * 1000) / 1000

    val sinp = sqrt(1 + 2 * (qw * qy - qx * qz))
    val cosp = sqrt(1 - 2 * (qw * qy - qx * qz))
    val pitch = ceil((atan2(sinp, cosp) - Math.PI / 2) * 1000) / 1000

    val siny_cosp = 2 * (qw * qz + qx * qy)
    val cosy_cosp = 1 - 2 * (qy * qy + qz * qz)
    val yaw = ceil(atan2(siny_cosp, cosy_cosp) * 1000) / 1000

    return doubleArrayOf(roll, pitch, yaw)
  }

  private fun sendData() {
    if (!isConnected.value) {
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

  override fun onMessage(message: String) {
    Log.d("GameActivity", ">Received: $message")
  }

  override fun onDisconnected() {
    // Redirect to the main activity
    Log.d("GameActivity", ">>>Disconnected")
    isConnected.value = false

    intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
  }
}


/**
 * Composable functions
 */

@Composable
fun SensorData(orientationViewModel: OrientationViewModel, calibrate: () -> Unit) {
  // Display the 3 orientation angles.
  val values by orientationViewModel.orientation.collectAsState()

  val string: String = """
    ${"x: %.3f".format(Math.toDegrees(values.x))}
    ${"y: %.3f".format(Math.toDegrees(values.z))}
    ${"z: %.3f".format(Math.toDegrees(values.y))}
  """.trimIndent()

  Row {
    Column {
      Text(text = "Orientation Angles:")
      Text(text = string, modifier = Modifier.padding(30.dp))
    }

    Column (
      Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Button(
        onClick = calibrate,
        modifier = Modifier.padding(10.dp)
      ) {
        Text(text = "Calibrate")
      }
    }
  }
}
