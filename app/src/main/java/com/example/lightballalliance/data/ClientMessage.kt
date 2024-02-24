package com.example.lightballalliance.data

import com.example.lightballalliance.WebSocketClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

sealed class ClientMessage {
  // Player name (not JSON)
  data class Name(val name: String) : ClientMessage()
  // player_rotation_updated
  @Serializable
  data class RotationUpdated(val x: Float, val y: Float, val z: Float) : ClientMessage()
  // player_ready
  data object Ready : ClientMessage()
  // enemy_shot
  @Serializable
  data class EnemyShot(val id: Int) : ClientMessage()
}

fun sendClientMessage(message: ClientMessage) {
  when (message) {
    // This is the only one that is not JSON-formatted
    is ClientMessage.Name -> {
      WebSocketClient.send(message.name)
    }

    is ClientMessage.RotationUpdated -> {
      val json = Json.encodeToJsonElement(message).toString()
      WebSocketClient.send("""{"type": "player_rotation_updated", "data": $json}""")
    }

    is ClientMessage.Ready -> {
      WebSocketClient.send("""{"type": "player_ready", "data": ""}""")
    }

    is ClientMessage.EnemyShot -> {
      val json = Json.encodeToJsonElement(message).toString()
      WebSocketClient.send("""{"type": "enemy_shot", "data": $json}""")
    }
  }
}
