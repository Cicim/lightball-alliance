package com.example.sensorsexample2

interface WebSocketListener {
  fun onConnected()
  fun onMessage(message: String)
  fun onDisconnected()
}