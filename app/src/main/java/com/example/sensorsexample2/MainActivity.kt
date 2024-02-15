package com.example.sensorsexample2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensorsexample2.ui.theme.SensorsExample2Theme

class MainActivity : ComponentActivity(), SensorEventListener, WebSocketListener {
  private lateinit var sensorManager: SensorManager
  private val accelerometerReading = FloatArray(3)
  private val magnetometerReading = FloatArray(3)
  private val rotationMatrix = FloatArray(9)
  private val orientationAngles = FloatArray(3)

  private val webSocketClient = WebSocketClient("ws://localhost:8080/ws")

  /**
   * SENSOR MANAGER
   * Retrieve the data from the accelerometer and magnetometer sensors
   * and continuously update the UI with the orientation angles.
   */

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      SensorsExample2Theme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          SensorData(orientationAngles)
          ConnectButtons(
            onClickConnect = { webSocketClient.connect(this) },
            onClickDisconnect = { webSocketClient.disconnect() }
          )
        }
      }
    }

    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    // Do something here if sensor accuracy changes.
    // You must implement this callback in your code.
  }

  override fun onResume() {
    super.onResume()

    // Get updates from the accelerometer and magnetometer at a constant rate.
    // To make batch operations more efficient and reduce power consumption,
    // provide support for delaying updates to the application.
    //
    // In this example, the sensor reporting delay is small enough such that
    // the application receives an update before the system checks the sensor
    // readings again.
    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
      sensorManager.registerListener(
        this,
        accelerometer,
        SensorManager.SENSOR_DELAY_NORMAL,
        SensorManager.SENSOR_DELAY_UI
      )
    }
    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
      sensorManager.registerListener(
        this,
        magneticField,
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
    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
      System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
      System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
    }

    updateOrientationAngles()

    // Update the UI with the new orientation angles
//    setContent {
//      SensorsExample2Theme {
//        SensorData(orientationAngles)
//      }
//    }
  }

  // Compute the three orientation angles based on the most recent readings from
  // the device's accelerometer and magnetometer.
  private fun updateOrientationAngles() {
    // Update rotation matrix, which is needed to update orientation angles.
    SensorManager.getRotationMatrix(
      rotationMatrix,
      null,
      accelerometerReading,
      magnetometerReading
    )

    // "rotationMatrix" now has up-to-date information.

    SensorManager.getOrientation(rotationMatrix, orientationAngles)

    // "orientationAngles" now has up-to-date information.
  }

  /**
   * WEBSOCKET CLIENT
   * Connect to the server and send the orientation angles.
   */


  override fun onConnected() {
    // Handle connection
  }

  override fun onMessage(message: String) {
    // Handle received message
  }

  override fun onDisconnected() {
    // Handle disconnection
  }
}

@Composable
fun SensorData(orientationAngles: FloatArray) {
  // Display the 3 orientation angles.
  val string: String = """
    ${"Azimuth: %.2f".format(Math.toDegrees(orientationAngles[0].toDouble()))}
    ${"Pitch: %.2f".format(Math.toDegrees(orientationAngles[1].toDouble()))}
    ${"Roll: %.2f".format(Math.toDegrees(orientationAngles[2].toDouble()))}
  """.trimIndent()

  Text(text = "Orientation Angles:")
  Text(text = string, modifier = Modifier.padding(30.dp))
}

// Create composable button to connect/disconnect from the server.
@Composable
fun ConnectButtons(onClickConnect: () -> Unit, onClickDisconnect: () -> Unit) {
  Column (
    Modifier.fillMaxHeight(),
    verticalArrangement = Arrangement.Bottom
  ) {
    Row (
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center
    ) {
      Button(
        onClick = onClickConnect,
        enabled = true,
        modifier = Modifier.padding(2.dp)
      ) {
        Text(text = "Connect")
      }

      Button(
        onClick = onClickDisconnect,
        enabled = true,
        modifier = Modifier.padding(2.dp)
      ) {
        Text(text = "Disconnect")
      }
    }
  }
}