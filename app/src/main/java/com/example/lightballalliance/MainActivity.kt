package com.example.lightballalliance

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.lightballalliance.data.GameMessage

class MainActivity : ComponentActivity(), WebSocketListener {
  // View model
  private val viewModel by viewModels<MainViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the listener for the WebSocket client
    WebSocketClient.setMainListener(this@MainActivity)

    setContent {
      MainScreen(viewModel)
    }
  }

  // This function is called when the user confirms the name.
  private fun navigateToGameActivity() {
    // Navigate to the game activity.
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
    viewModel.setConnected()
  }

  override fun onMessage(message: GameMessage) {
    // Handle received message
    if (message is GameMessage.AskName) {
      WebSocketClient.playerName.value = ""

      // If the server asks again for the name, reset the nameConfirmed variable
      // and ask the user to enter the name again.
      viewModel.askNameAgain()

      return
    }

    // If the username has been accepted, continue to the game activity.
    if (message is GameMessage.Ready) {
      navigateToGameActivity()
      return
    }
  }

  override fun onDisconnected() {
    // Handle disconnection
    Log.d("MainActivity", ">>>Disconnected!")
    viewModel.setDisconnected()
  }

  override fun onError() {
    // Handle timeout
    Log.d("MainActivity", ">>>Connection Error!")
    viewModel.setConnectionError()
  }

  override fun onResume() {
    super.onResume()

    // When the activity is resumed, clear the state of the view model.
    viewModel.clearState()
    // The address and username should stay the same for when you want to reconnect.

    Log.d("MainActivity", ">>>Resumed!")
  }
}
