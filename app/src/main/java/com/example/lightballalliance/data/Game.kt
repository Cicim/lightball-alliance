package com.example.lightballalliance.data

class Game {
  private val players: List<Player> = emptyList()
  private val enemies: List<Enemy> = emptyList()


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
}
