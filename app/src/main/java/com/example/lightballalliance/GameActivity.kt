package com.example.lightballalliance

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.lightballalliance.ui.theme.lightballallianceTheme

class GameActivity : AppCompatActivity(), WebSocketListener {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WebSocketClient.setGameListener(this@GameActivity)

    setContent {
      // A surface container using the 'background' color from the theme
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        lightballallianceTheme {
          Text(
            text = "Game Activity",
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }
    }
  }

  override fun onConnected() {
    // Nothing to do here
  }

  override fun onMessage(message: String) {
    Log.d("GameActivity", ">Received: $message")
  }

  override fun onDisconnected() {
    // Redirect to the main activity
    Log.d("GameActivity", ">>>Disconnected")
    intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
  }
}
