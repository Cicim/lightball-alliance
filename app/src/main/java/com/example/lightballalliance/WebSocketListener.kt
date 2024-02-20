package com.example.lightballalliance

import com.example.lightballalliance.data.GameMessage

interface WebSocketListener {
  fun onConnected()
  fun onMessage(message: GameMessage)
  fun onDisconnected()
}