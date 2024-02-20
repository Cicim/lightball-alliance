package com.example.lightballalliance.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


sealed class GameMessage {
  // game_started
  @Serializable
  data class GameStarted(val players: List<PlayerData>)
  // game_over
  data class GameOver(val reason: String)
  // time_sync
  data class TimeSync(val time: Int)
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
  )
  // enemy_damaged
  @Serializable
  data class EnemyDamaged(val id: Int, val health: Int)
  // enemy_removed
  data class EnemyRemoved(val id: Int)
  // player_rotation_updated
  @Serializable
  data class PlayerRotationUpdated(val username: String, val rotation: Vec3)
  // player_score_updated
  @Serializable
  data class PlayerScoreUpdated(val username: String, val score: Int)
  // player_damaged
  @Serializable
  data class PlayerDamaged(val username: String, val health: Int)
}

fun parseGameMessage(message: String): Any {
  // Parse the message to get type and data
  // Messages are of the form {"type": "message_type", "data": <json_data>}
  val json = Json.parseToJsonElement(message)
  val type = json.jsonObject["type"]!!.jsonPrimitive.content
  val data = json.jsonObject["data"].toString()
  println("Type: $type, Data: $data")

  return when (type) {
    "game_started" -> { Json.decodeFromString<GameMessage.GameStarted>(data) }
    "game_over" -> {
      GameMessage.GameOver(data)
    }
    "time_sync" -> {
      GameMessage.TimeSync(data.toInt())
    }
    "enemy_added" -> { Json.decodeFromString<GameMessage.EnemyAdded>(data) }
    "enemy_damaged" -> { Json.decodeFromString<GameMessage.EnemyDamaged>(data) }
    "enemy_removed" -> {
      GameMessage.EnemyRemoved(data.toInt())
    }
    "player_rotation_updated" -> { Json.decodeFromString<GameMessage.PlayerRotationUpdated>(data) }
    "player_score_updated" -> { Json.decodeFromString<GameMessage.PlayerScoreUpdated>(data) }
    "player_damaged" -> { Json.decodeFromString<GameMessage.PlayerDamaged>(data) }
    else -> { throw IllegalArgumentException("Invalid message type") }
  }
}

@Serializable
data class Vec3(val x: Float, val y: Float, val z: Float)

@Serializable
data class PlayerData(
  val username: String,
  val health: Int,
  val score: Int,
  val position: Vec3,
  val rotation: Vec3
)
