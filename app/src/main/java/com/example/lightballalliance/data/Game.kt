package com.example.lightballalliance.data

import android.util.Log
import com.example.lightballalliance.WebSocketClient
import com.example.lightballalliance.eulerAnglesToQuaternion
import com.example.lightballalliance.multiplyQuaternions
import com.example.lightballalliance.quaternionToEulerAngles
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class Game (
  playersData: List<PlayerData>
) {
  private var players: MutableList<Player> = mutableListOf()
  private var enemies: HashMap<Int, Enemy> = hashMapOf()
  private var time: Int = 0
  private var gameOverReason: GameOverReason? = null

  // Whether the last shoot was successful
  var lastShootResult: Boolean? = null
  // The time in frames before you can shoot again.
  var shootTimer: Int = 0

  // Vector the represents the position of the camera
  private var eyePosition: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
  // Unit vector that represents the orientation of the camera
  private var centerVersor: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)

  // Whether the camera yaw should be inverted (front of back)
  var yawMultiplier = 1.0f
  // Last quaternion measurement, to be replaced when the interpolation ends
  private var currentOrientationQuarternion: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

  init {
    Log.d("Game", "Game created")
    Log.d("Game", "Player 0: ${playersData[0]}")
    Log.d("Game", "Player 1: ${playersData[1]}")

    // Initialize the variables
    for (data in playersData) {
      val position = floatArrayOf(data.position.x, data.position.y, data.position.z)
      val initialRotation = doubleArrayOf(
        data.rotation.x.toDouble(), data.rotation.y.toDouble(), data.rotation.z.toDouble())

      val player = Player(data.username, position, DoubleArray(3), initialRotation)
      players.add(player)
    }
  }

  /**
   * Methods to manage the camera position and orientation.
   */

  fun setCameraEye(x: Float, y: Float, z: Float) {
    eyePosition = floatArrayOf(x, y, z)
  }

  // Function to set the camera orientation according according to the
  // orientation quaternion passed in as input.
  fun setCameraOrientation(quaternion: FloatArray) {
    val finalOrientationAngles = quaternionToEulerAngles(quaternion)
    // val roll = finalOrientationAngles[2]
    val pitch = finalOrientationAngles[0]
    val yaw = yawMultiplier * finalOrientationAngles[1]

    centerVersor[0] = cos(yaw).toFloat() * cos(pitch).toFloat()
    centerVersor[1] = sin(pitch).toFloat()
    centerVersor[2] = -sin(yaw).toFloat() * cos(pitch).toFloat()
  }

  fun setNewOrientationQuaternion(quaternion: FloatArray) {
    currentOrientationQuarternion = quaternion
  }

  fun getNewOrientationQuaternion(): FloatArray {
    return currentOrientationQuarternion
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
   * Functions for shooting.
   */

  private fun findTarget(): Int? {
    val cameraPosition = getCameraEye()

    for (enemy in enemies.values) {
      val (ex, ey, ez) = enemy.getPosition(time)

      // Step along the camera orientation vector starting from the camera eye
      for (i in 0 until 80) {
        val step = 0.15f * i
        val rx = cameraPosition[0] + step * centerVersor[0]
        val ry = cameraPosition[1] + step * centerVersor[1]
        val rz = cameraPosition[2] + step * centerVersor[2]

        // Until you hit a target
        val distance = sqrt((rx - ex).pow(2) + (ry - ey).pow(2) + (rz - ez).pow(2))
        if (distance < 0.2f) {
          return enemy.getId()
        }
      }
    }

    return null
  }

  fun shoot() {
    // You cannot shoot if the timer is not 0
    if (shootTimer > 0) {
      return
    }

    val target = findTarget()
    lastShootResult = if (target != null) {
      sendClientMessage(ClientMessage.EnemyShot(target))
      true
    } else {
      false
    }

    // One shot every second
    shootTimer = 60
  }

  /**
   * Returns the current orientation of the ally in a form that can be
   * used by the OpenGL renderer.
   *
   * That is, an axis of rotation and an angle of rotation.
   */
  fun getAllyRotation(): Pair<FloatArray, Float> {
    val ally = getAllyPlayer()
    // Convert the current ally rotation a quaternion
    val (x, y, z) = ally.getRotation()
    val q = eulerAnglesToQuaternion(x, y, z)
    // Get the initial ally rotation to a quaternion
    val (ix, iy, iz) = ally.getInitialRotation()
    val iq = eulerAnglesToQuaternion(ix, iy, iz)

    // Apply the initial rotation, then the current rotation
    val mq = multiplyQuaternions(iq, q)
    val angles = quaternionToEulerAngles(mq)

    // Change the angles to the OpenGL format
    val pitch = angles[0]
    val yaw = angles[1]

    if (abs(pitch) + abs(yaw) <= 0.1) {
      return Pair(floatArrayOf(0.0f, 1.0f, 0.0f), 0.0f)
    }

    val rq = eulerAnglesToQuaternion(0.0, -yaw, pitch)

    // Convert the quaternion to axis, angle, and return the angle
    val angle = 2 * acos(rq[3])
    val s = sin(angle / 2)
    val axis = floatArrayOf(rq[0] / s, rq[1] / s, rq[2] / s)

    return Pair(axis, angle)
  }


  /**
   * Methods to manage the list of enemies.
   */

  // Add a new enemy to the scene
  fun addEnemy(enemy: Enemy) {
    enemies[enemy.getId()] = enemy
  }

  // Remove an enemy by id from the scene
  fun removeEnemy(enemy: Int) {
    enemies.remove(enemy)
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

  fun getYourPlayer(): Player {
    WebSocketClient.playerName.value.let { name ->
      return players.find { it.getUsername() == name }!!
    }
  }

  fun getAllyPlayer(): Player {
    WebSocketClient.playerName.value.let { name ->
      return players.find { it.getUsername() != name }!!
    }
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

  // Set the game as over.
  fun setGameOver(reason: GameOverReason) {
    gameOverReason = reason
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
    return enemies.values.toList()
  }

  // Return the enemy with the given id.
  fun getEnemy(id: Int): Enemy? {
    return enemies[id]
  }

  // Return the current time.
  fun getTime(): Int {
    return time
  }

  // Return whether the game is over.
  fun isGameOver(): Boolean {
    return gameOverReason != null
  }

  // Return the reason why the game is over.
  fun getGameOverReason(): GameOverReason? {
    return gameOverReason
  }
}
