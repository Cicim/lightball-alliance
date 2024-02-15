package com.example.sensorsexample2

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.runBlocking

class WebSocketClient(private val host: String, private val port: Int, private val path: String) {

  private val client = HttpClient(CIO) {
    install(WebSockets) {
      pingInterval = 20_000
    }
  }

  fun connect(listener: WebSocketListener) {
    Log.d("WebSocketClient", ">>>Connecting to $host:$port$path")
    runBlocking {
      client.ws(
        method = HttpMethod.Get,
        host = host,
        port = port,
        path = path
      ) {
        listener.onConnected()

        try {
          send(Frame.Text("Initial handshake message."))
          Log.d("WebSocketClient", ">Sent: Initial handshake message.")

          while (true) {
            val frame = incoming.receive()
            if (frame is Frame.Text) {
              val text = frame.readText()
              listener.onMessage(text)
            }
          }
        } catch (e: Exception) {
          Log.e("WebSocketClient", "Error: $e")
          listener.onDisconnected()
        }
      }
    }
  }

  fun send(message: String) {
    runBlocking {
      client.ws(
        method = HttpMethod.Get,
        host = host,
        port = port,
        path = path
      ) {
        send(Frame.Text(message))
        Log.d("WebSocketClient", ">Sent: $message")
      }
    }
  }

  fun disconnect(listener: WebSocketListener) {
    client.close()
    listener.onDisconnected()
  }
}