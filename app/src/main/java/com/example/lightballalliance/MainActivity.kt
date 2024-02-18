package com.example.lightballalliance

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lightballalliance.ui.theme.lightballallianceTheme

class MainActivity : ComponentActivity(), WebSocketListener {
  private val isConnected = mutableStateOf(false)
  private val address = mutableStateOf("ws://10.0.2.2:8080")

  private val askName = mutableStateOf("NO_NAME")
  private val nameConfirmed = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the variables.
    isConnected.value = false
    nameConfirmed.value = false
    WebSocketClient.setMainListener(this@MainActivity)

    setContent {
      lightballallianceTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Column (
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
          ) {
            AddressTextBox(
              address = address.value,
              onAddressChange = { address.value = it },
              isConnected = isConnected.value
            )

            NameTextBox(
              askName = askName.value,
              onNameChange = { askName.value = it },
              isConnected = isConnected.value,
              confirmName = {
                WebSocketClient.send(askName.value)
                nameConfirmed.value = true
                navigateToGameActivity()
              }
            )

            ConnectButtons(
              onClickConnect = { WebSocketClient.connect(address.value) },
              onClickDisconnect = { WebSocketClient.disconnect() },
              isConnected = isConnected.value
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
  }

  override fun onMessage(message: String) {
    // Handle received message
    Log.d("MainActivity", ">Received: $message")

    // Parse the JSON message that is received from the server.
    val regex = """^\{"type":\s*"(.*)",\s*"data":\s*(.*)\}$""".toRegex()

    val matchResult = regex.find(message)
    if (matchResult != null) {
      val (type, data) = matchResult.destructured
//      Log.d("MainActivity", ">>Type: $type, Data: $data")

      if (type == "ask_name") {
        askName.value = ""
      }
    }
  }

  override fun onDisconnected() {
    // Handle disconnection
    Log.d("MainActivity", ">>>Disconnected!")
    isConnected.value = false
  }
}


/**
 * Composable functions
 */

@Composable
fun AddressTextBox(address: String, onAddressChange: (String) -> Unit, isConnected: Boolean) {
  // Create composable text box to enter the server address.
  Row (
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    OutlinedTextField(
      value = address,
      onValueChange = onAddressChange,
      label = { Text("Server Address (ws://host:port/path)") },
      enabled = !isConnected
    )
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
