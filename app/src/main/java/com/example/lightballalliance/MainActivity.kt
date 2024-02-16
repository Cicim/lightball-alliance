package com.example.lightballalliance

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lightballalliance.ui.theme.lightballallianceTheme

class MainActivity : ComponentActivity(), SensorEventListener, WebSocketListener {
  private lateinit var sensorManager: SensorManager
  private lateinit var webSocketClient: WebSocketClient

  private val accelerometerReading = FloatArray(3)
  private val prevAccelerometerReading = FloatArray(3)
  private val magnetometerReading = FloatArray(3)
  private val prevMagnetometerReading = FloatArray(3)

  private val rotationMatrix = FloatArray(9)
  private val orientationAngles = FloatArray(3)

  private val orientationViewModel: OrientationViewModel by viewModels()
  private val isConnected = mutableStateOf(false)
  private val address = mutableStateOf("ws://10.0.2.2:8080/ws")


  /**
   * SENSOR MANAGER
   * Retrieve the data from the accelerometer and magnetometer sensors
   * and continuously update the UI with the orientation angles.
   */

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the variables.
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    isConnected.value = false

    setContent {
      lightballallianceTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          SensorData(orientationViewModel)

          Column (
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
          ) {
            AddressTextBox(address = address.value, onAddressChange = { address.value = it })

            ConnectButtons(
              onClickConnect = {
                webSocketClient = WebSocketClient(address.value)
                webSocketClient.connect(this@MainActivity)
              },
              onClickDisconnect = { webSocketClient.disconnect(this@MainActivity) },
              isConnected = isConnected.value
            )
          }
        }
      }
    }
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
    var changed = false

    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
      if (!event.values.contentEquals(prevAccelerometerReading)) {
        System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        System.arraycopy(event.values, 0, prevAccelerometerReading, 0, prevAccelerometerReading.size)
        changed = true
      }
    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
      if (!event.values.contentEquals(prevMagnetometerReading)) {
        System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        System.arraycopy(event.values, 0, prevMagnetometerReading, 0, prevMagnetometerReading.size)
        changed = true
      }
    }

    if (changed) {
      // Update the orientation angles
      updateOrientationAngles()

      // Update the UI with the new orientation angles
      orientationViewModel.updateOrientation(
        orientationAngles[0],
        orientationAngles[1],
        orientationAngles[2]
      )

      // Send the orientation angles to the server.
      sendData()
    }
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
    Log.d("WebSocketClient", ">>>Connected!")
    isConnected.value = true
  }

  override fun onMessage(message: String) {
    // Handle received message
    Log.d("WebSocketClient", ">Received: $message")
  }

  override fun onDisconnected() {
    // Handle disconnection
    Log.d("WebSocketClient", ">>>Disconnected!")
    isConnected.value = false
  }

  private fun sendData() {
    if (!isConnected.value) {
      return
    }

    // Send the orientation angles to the server.
    val azimuth = "%.2f".format(Math.toDegrees(orientationAngles[0].toDouble()))
    val pitch = "%.2f".format(Math.toDegrees(orientationAngles[1].toDouble()))
    val roll = "%.2f".format(Math.toDegrees(orientationAngles[2].toDouble()))
    val message = "Azimuth: $azimuth, Pitch: $pitch, Roll: $roll"
    webSocketClient.send(message)
  }
}

@Composable
fun SensorData(orientationViewModel: OrientationViewModel) {
  // Display the 3 orientation angles.
  val values by orientationViewModel.orientation.collectAsState()

  val string: String = """
    ${"Azimuth: %.2f".format(Math.toDegrees(values.azimuth.toDouble()))}
    ${"Pitch: %.2f".format(Math.toDegrees(values.pitch.toDouble()))}
    ${"Roll: %.2f".format(Math.toDegrees(values.roll.toDouble()))}
  """.trimIndent()

  Text(text = "Orientation Angles:")
  Text(text = string, modifier = Modifier.padding(30.dp))
}

@Composable
fun AddressTextBox(address: String, onAddressChange: (String) -> Unit) {
  // Create composable text box to enter the server address.
  Row (
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    OutlinedTextField(
      value = address,
      onValueChange = onAddressChange,
      label = { Text("Server Address (ws://host:port/path)") },
    )
  }
}

// Create composable button to connect/disconnect from the server.
@Composable
fun ConnectButtons(onClickConnect: () -> Unit, onClickDisconnect: () -> Unit, isConnected: Boolean) {
  Row (
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    Button(
      onClick = onClickConnect,
      enabled = !isConnected,
      modifier = Modifier.padding(2.dp)
    ) {
      Text(text = "Connect")
    }

    Button(
      onClick = onClickDisconnect,
      enabled = isConnected,
      modifier = Modifier.padding(2.dp)
    ) {
      Text(text = "Disconnect")
    }
  }
}