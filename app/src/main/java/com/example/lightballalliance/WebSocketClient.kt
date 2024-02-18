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

object WebSocketClient {
  private var webSocketSession: WebSocketSession? = null
  private var mainListener: WebSocketListener? = null
  private var gameListener: WebSocketListener? = null
  private var job: Job? = null

  private lateinit var client: HttpClient

  fun setMainListener(listener: WebSocketListener) {
     mainListener = listener
  }

  fun setGameListener(listener: WebSocketListener) {
     gameListener = listener
  }

  fun connect(urlGiven: String) {
    client = HttpClient(CIO) {
      install(WebSockets)
    }

    Log.d("WebSocketClient", ">>>Connecting to $urlGiven")

    job = CoroutineScope(Dispatchers.IO).launch {
      try {
        webSocketSession = client.webSocketSession {
          url(urlGiven)
        }

        mainListener?.onConnected()
        Log.d("WebSocketClient", ">>>Connected to $urlGiven")

        while (isConnected()) {
          val message = webSocketSession?.incoming?.receive()
          if (message is Frame.Text) {
            // Parse the JSON message that is received from the server.
            val regex = """^\{"type":\s*"(.*)",\s*"data":\s*(.*)\}$""".toRegex()

            val matchResult = regex.find(message.readText())
            if (matchResult != null) {
              val (type, _) = matchResult.destructured

              if (type == "ask_name") {
                mainListener?.onMessage(type)
              }
              else {
                gameListener?.onMessage(message.readText())
              }
            }
          }
        }
      } catch (e: Exception) {
        Log.d("WebSocketClient", ">>>Error: ${e.message}")
        disconnect()
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

  fun disconnect() {
    job?.cancel()
    runBlocking {
      webSocketSession?.close()
      client.close()
    }
    mainListener?.onDisconnected()
    gameListener?.onDisconnected()
  }

  private fun isConnected(): Boolean {
    return webSocketSession?.isActive ?: false
  }
}