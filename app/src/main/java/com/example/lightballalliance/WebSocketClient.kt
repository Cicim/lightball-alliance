package com.example.lightballalliance

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WebSocketClient(private val urlGiven: String) {

  private val client = HttpClient(CIO) {
    install(WebSockets)
  }

  private var webSocketSession: WebSocketSession? = null
  private var job: Job? = null

  fun connect(listener: WebSocketListener) {
    Log.d("WebSocketClient", ">>>Connecting to $urlGiven")

    job = CoroutineScope(Dispatchers.IO).launch {
      try {
        webSocketSession = client.webSocketSession {
          url(urlGiven)
        }

        listener.onConnected()
        Log.d("WebSocketClient", ">>>Connected to $urlGiven")

        while (isConnected()) {
          val message = webSocketSession?.incoming?.receive()
          if (message is Frame.Text) {
            listener.onMessage(message.readText())
          }
        }
      } catch (e: Exception) {
        Log.d("WebSocketClient", ">>>Error: ${e.message}")
        disconnect(listener)
      }
    }
  }

  fun send(message: String) {
    if (isConnected()) {
      CoroutineScope(Dispatchers.IO).launch {
        webSocketSession?.send(Frame.Text(message))
        Log.d("WebSocketClient", ">Sent: $message")
      }
    }
  }

  fun disconnect(listener: WebSocketListener) {
    job?.cancel()
    runBlocking {
      webSocketSession?.close()
      client.close()
    }
    listener.onDisconnected()
  }

  private fun isConnected(): Boolean {
    return webSocketSession?.isActive ?: false
  }
}