package com.example.lightballalliance.data

class Game (
  playersData: List<PlayerData>
) {
  private val players: List<Player> = emptyList()
  private val enemies: List<Enemy> = emptyList()
  private var time: Int = 0

  init {
    // Initialize the variables
    for (data in playersData) {
      val position = doubleArrayOf(data.position.x, data.position.y, data.position.z)
      val rotation = doubleArrayOf(data.rotation.x, data.rotation.y, data.rotation.z)
      val initialRotation = rotation.clone()

      val player = Player(data.username, position, rotation, initialRotation)
      players.plus(player)
    }
  }


  /**
   * Methods to manage the list of enemies.
   */

  // Add a new enemy to the scene
  fun addEnemy(enemy: Enemy) {
    enemies.plus(enemy)
  }

  // Remove an enemy by id from the scene
  fun removeEnemy(enemy: Int) {
    // Find the enemy with the given id
    val index = enemies.indexOfFirst { it.getId() == enemy }
    enemies.minus(index)
  }


  /**
   * Methods to manage the list of players.
   */

  // Add the player to the list of players.
  fun addPlayer(player: Player) {
    players.plus(player)
  }

  // Remove the player from the list of players.
  fun removePlayer(player: Player) {
    players.minus(player)
  }

  /**
   * Setters
   */

  // Synchronize the time with the server.
  fun syncTime(time: Int) {
    this.time = time
  }

  // Update the time.
  fun increaseTime() {
    time += 100
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
