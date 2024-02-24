package com.example.lightballalliance.data

import android.util.Log
import kotlin.math.cos
import kotlin.math.sin

class Game (
  playersData: List<PlayerData>
) {
  private var players: MutableList<Player> = mutableListOf()
  private var enemies: MutableList<Enemy> = mutableListOf()
  private var time: Int = 0

  // Vector the represents the position of the camera
  private var eyePosition: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
  // Versor that represents the orientation of the camera
  private var centerVersor: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)


  init {
    Log.d("Game", "Game created")
    Log.d("Game", "Player 0: ${playersData[0]}")
    Log.d("Game", "Player 1: ${playersData[1]}")

    // Initialize the variables
    for (data in playersData) {
      val position = doubleArrayOf(data.position.x, data.position.y, data.position.z)
      val rotation = doubleArrayOf(data.rotation.x, data.rotation.y, data.rotation.z)
      val initialRotation = rotation.clone()

      val player = Player(data.username, position, rotation, initialRotation)
      players.add(player)
    }

    // Create an enemy that stays at the origin.
    val enemy = Enemy(999, 100, 0xFFFFFF, 0, 0.0, doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(0.0, 0.0, 0.0))
    enemies.add(enemy)
  }

  /**
   * Methods to manage the camera position and orientation.
   */

  fun setCameraEye(x: Float, y: Float, z: Float) {
    eyePosition = floatArrayOf(x, y, z)
  }

  // Function to set the camera orientation according to the orientation angles
  // of the device, using roll, pitch, and yaw
  fun setCameraOrientation(roll: Double, pitch: Double, yaw: Double) {
    centerVersor[0] = cos(yaw).toFloat() * cos(pitch).toFloat()
    centerVersor[1] = sin(pitch).toFloat()
    centerVersor[2] = -sin(yaw).toFloat() * cos(pitch).toFloat()
  }

  fun getCameraEye(): FloatArray {
    return eyePosition
  }

  fun getCameraCenter(): FloatArray {
    return floatArrayOf(
      eyePosition[0] + centerVersor[0],
      eyePosition[1] + centerVersor[1],
      eyePosition[2] + centerVersor[2])
  }


  /**
   * Methods to manage the list of enemies.
   */

  // Add a new enemy to the scene
  fun addEnemy(enemy: Enemy) {
    enemies.add(enemy)
  }

  // Remove an enemy by id from the scene
  fun removeEnemy(enemy: Int) {
    // Find the enemy with the given id
    val index = enemies.indexOfFirst { it.getId() == enemy }
    enemies.removeAt(index)
  }


  /**
   * Methods to manage the list of players.
   */

  // Add the player to the list of players.
  fun addPlayer(player: Player) {
    players.add(player)
  }

  // Remove the player from the list of players.
  fun removePlayer(player: Player) {
    players.remove(player)
  }

  /**
   * Setters
   */

  // Synchronize the time with the server.
  fun syncTime(time: Int) {
    this.time = time
  }

  // Update the time.
  fun increaseTime(add: Int) {
    time += add
  }


  /**
   * Getters
   */

  // Return the list of players.
  fun getPlayers(): List<Player> {
    return players
  }

  // Return the player with the given name.
  fun getPlayer(name: String): Player {
    return players.find { it.getUsername() == name }!!
  }

  // Return the list of enemies.
  fun getEnemies(): List<Enemy> {
    return enemies
  }

  // Return the enemy with the given id.
  fun getEnemy(id: Int): Enemy {
    return enemies.find { it.getId() == id }!!
  }

  // Return the current time.
  fun getTime(): Int {
    return time
  }
}
