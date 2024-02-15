package com.example.sensorsexample2

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WebSocketClient(private val host: String, private val port: Int, private val path: String) {

  private val client = HttpClient(CIO) {
    install(WebSockets) {
      pingInterval = 20_000
    }
  }

  suspend fun connect(listener: WebSocketListener) {
    Log.d("WebSocketClient", ">>>Connecting to $host:$port$path")

      client.ws(
        method = HttpMethod.Get,
        host = host,
        port = port,
        path = path
      ) {
        listener.onConnected()

        for (i in 1..10) {
          send(Frame.Text("Hello, world!"))
          Log.d("WebSocketClient", ">Sent: Hello, world!")
        }

        for (frame in incoming) {
          if (frame is Frame.Text) {
            listener.onMessage(frame.readText())
          }
        }
      }


  }

  fun disconnect(listener: WebSocketListener) {
    client.close()
    listener.onDisconnected()
  }
}