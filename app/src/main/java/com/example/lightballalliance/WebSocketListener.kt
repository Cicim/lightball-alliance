package com.example.lightballalliance

interface WebSocketListener {
  fun onConnected()
  fun onMessage(message: String)
  fun onDisconnected()
}