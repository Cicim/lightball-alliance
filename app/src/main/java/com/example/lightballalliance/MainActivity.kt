package com.example.lightballalliance

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lightballalliance.data.ClientMessage
import com.example.lightballalliance.data.GameMessage
import com.example.lightballalliance.data.sendClientMessage
import com.example.lightballalliance.ui.theme.lightballallianceTheme
import java.io.IOException

class MainActivity : ComponentActivity(), WebSocketListener {
  private val isConnected = mutableStateOf(false)
  private val isConnecting = mutableStateOf(false)
  private val address = mutableStateOf("ws://10.0.2.2:8080")
  private val nameConfirmed = mutableStateOf(false)
  private val connectionError = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the variables.
    isConnected.value = false
    nameConfirmed.value = false
    WebSocketClient.setMainListener(this@MainActivity)

    setContent {
      lightballallianceTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colorScheme.background) {
          Row (
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
          ) {
            Logo()
          }

          Column (
            Modifier
              .fillMaxSize()
              .padding(top = 100.dp),
            verticalArrangement = Arrangement.Center
          ) {
            AddressTextBox(
              address = address.value,
              onAddressChange = { address.value = it },
              isConnected = isConnected.value,
              connectionError = connectionError.value
            )

            NameTextBox(
              askName = WebSocketClient.playerName.value,
              onNameChange = { WebSocketClient.playerName.value = it },
              isConnected = isConnected.value,
              confirmName = {
                sendClientMessage(ClientMessage.Name(WebSocketClient.playerName.value))
                nameConfirmed.value = true
                navigateToGameActivity()
              }
            )

            ConnectButtons(
              onClickConnect = {
                connectionError.value = false
                isConnecting.value = true
                WebSocketClient.connect(address.value)
              },
              onClickDisconnect = { WebSocketClient.disconnect() },
              isConnected = isConnected.value,
              isConnecting = isConnecting.value
            )
          }
        }
      }
    }
  }

  private fun navigateToGameActivity() {
    // Navigate to the game activity.
    // This function is called when the user confirms the name.
    val intent = Intent(this, GameActivity::class.java)
    startActivity(intent)
  }


  /**
   * WEBSOCKET CLIENT
   * Connect to the server and send the orientation angles.
   */

  override fun onConnected() {
    // Handle connection
    Log.d("MainActivity", ">>>Connected!")
    isConnected.value = true
    isConnecting.value = false
  }

  override fun onMessage(message: GameMessage) {
    // Handle received message
    if (message is GameMessage.AskName)
      WebSocketClient.playerName.value = ""
  }

  override fun onDisconnected() {
    // Handle disconnection
    Log.d("MainActivity", ">>>Disconnected!")
    isConnected.value = false
    isConnecting.value = false
  }

  override fun onError() {
    // Handle timeout
    Log.d("MainActivity", ">>>Connection Error!")
    connectionError.value = true
  }
}


/**
 * Composable functions
 */

@Composable
fun AddressTextBox(address: String, onAddressChange: (String) -> Unit, isConnected: Boolean, connectionError: Boolean) {
  // Create composable text box to enter the server address.
  Column (
    Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    OutlinedTextField(
      value = address,
      onValueChange = onAddressChange,
      label = { Text("Server Address (ws://host:port/path)") },
      enabled = !isConnected,
      isError = connectionError
    )

    if (connectionError) {
      Text(
        text = "Cannot connect to the server.\nPlease check the address and try again.",
        color = Color.Red,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(10.dp),
      )
    }
  }
}

@Composable
fun NameTextBox(askName: String, onNameChange: (String) -> Unit, isConnected: Boolean, confirmName: () -> Unit) {
  // Create composable text box to enter the name.
  if (askName != "NO_NAME" && isConnected) {
    Column (
      Modifier.padding(10.dp)
    ) {
      Text(
        text = "Welcome to the game!\nChoose your name:",
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )

      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        OutlinedTextField(
          value = askName,
          onValueChange = onNameChange,
          label = { Text("Name") },
        )
      }

      Button(
        onClick = confirmName,
        modifier = Modifier
          .padding(bottom = 10.dp, top = 5.dp)
          .align(Alignment.CenterHorizontally)
      ) {
        Text(text = "Confirm")
      }
    }
  }
}

@Composable
fun ConnectButtons(
  onClickConnect: () -> Unit,
  onClickDisconnect: () -> Unit,
  isConnected: Boolean,
  isConnecting: Boolean
) {
  Row (
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    Button(
      onClick = onClickConnect,
      enabled = !isConnected && !isConnecting,
      modifier = Modifier.padding(2.dp)
    ) {
      if (isConnecting)
        Text(text = "Connecting...")
      else
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

@Composable
fun Logo() {
  // Load the image from the assets folder
  val context = LocalContext.current
  val bitmap = loadImageResource(context, "logo.png").resource.asAndroidBitmap()

  // Display the loaded bitmap
  Image(
    bitmap = bitmap.asImageBitmap(),
    contentDescription = "Lightball Alliance Logo",
    alignment = Alignment.TopCenter,
    modifier = Modifier
      .padding(top = 20.dp, bottom = 20.dp)
      .height(500.dp)
  )
}

@Composable
fun loadImageResource(context: Context, path: String): ImageAsset {
  return runCatching {
    val inputStream = context.assets.open(path)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    ImageAsset(bitmap.asImageBitmap())
  }.getOrElse {
    throw IOException("Could not load image from assets at $path")
  }
}

class ImageAsset(val resource: androidx.compose.ui.graphics.ImageBitmap)
