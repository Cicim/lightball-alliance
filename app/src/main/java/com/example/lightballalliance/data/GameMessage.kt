package com.example.lightballalliance.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class GameMessage {
  // Game communications
  // game_started
  @Serializable
  data class GameStarted(val players: List<PlayerData>) : GameMessage()
  // game_over
  data class GameOver(val reason: String) : GameMessage()
  // time_sync
  data class TimeSync(val time: Int) : GameMessage()
  // enemy_added
  @Serializable
  data class EnemyAdded(
    val id: Int,
    val color: Int,
    val health: Int,
    val source: Vec3,
    val target: Vec3,
    val startTime: Int,
    val speed: Double
  ) : GameMessage()
  // enemy_damaged
  @Serializable
  data class EnemyDamaged(val id: Int, val health: Int) : GameMessage()
  // enemy_removed
  data class EnemyRemoved(val id: Int) : GameMessage()
  // player_rotation_updated
  @Serializable
  data class PlayerRotationUpdated(val username: String, val rotation: Vec3) : GameMessage()
  // player_score_updated
  @Serializable
  data class PlayerScoreUpdated(val username: String, val score: Int) : GameMessage()
  // player_damaged
  @Serializable
  data class PlayerDamaged(val username: String, val health: Int) : GameMessage()

  // Server communications
  // ask_name
  data class AskName(val message: String) : GameMessage()
  // ready
  data class Ready(val message: String) : GameMessage()
  // waiting
  data class Waiting(val message: String) : GameMessage()
  // matched
  data class Matched(val username: String) : GameMessage()
}

fun parseGameMessage(message: String): GameMessage {
  // Parse the message to get type and data
  // Messages are of the form {"type": "message_type", "data": <json_data>}
  val json = Json.parseToJsonElement(message)
  val type = json.jsonObject["type"]!!.jsonPrimitive.content
  val data = json.jsonObject["data"].toString()
  println("Type: $type, Data: $data")

  return when (type) {
    "game_started" -> { Json.decodeFromString<GameMessage.GameStarted>(data) }
    "game_over" -> { GameMessage.GameOver(Json.decodeFromString<String>(data)) }
    "time_sync" -> { GameMessage.TimeSync(data.toInt()) }
    "enemy_added" -> { Json.decodeFromString<GameMessage.EnemyAdded>(data) }
    "enemy_damaged" -> { Json.decodeFromString<GameMessage.EnemyDamaged>(data) }
    "enemy_removed" -> { GameMessage.EnemyRemoved(data.toInt()) }
    "player_rotation_updated" -> { Json.decodeFromString<GameMessage.PlayerRotationUpdated>(data) }
    "player_score_updated" -> { Json.decodeFromString<GameMessage.PlayerScoreUpdated>(data) }
    "player_damaged" -> { Json.decodeFromString<GameMessage.PlayerDamaged>(data) }

    "ask_name" -> { GameMessage.AskName(Json.decodeFromString<String>(data)) }
    "waiting" -> { GameMessage.Waiting(Json.decodeFromString<String>(data)) }
    "ready" -> { GameMessage.Ready(Json.decodeFromString<String>(data)) }
    "matched" -> { GameMessage.Matched(Json.decodeFromString<String>(data)) }
    else -> {
      println("Unknown message type: $type")
      throw IllegalArgumentException("Unknown message type: $type")
    }
  }
}

@Serializable
data class Vec3(val x: Double, val y: Double, val z: Double)

@Serializable
data class PlayerData(
  val username: String,
  val health: Int,
  val score: Int,
  val position: Vec3,
  val rotation: Vec3
)
