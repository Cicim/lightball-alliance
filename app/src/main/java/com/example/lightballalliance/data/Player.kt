package com.example.lightballalliance.data

class Player (
  // Constructor
  private var username: String,
  private var position: DoubleArray,
  private var rotation: DoubleArray,
  private var initialRotation: DoubleArray
) {
  private var score: Int = 0
  private var health: Int = 100


  /**
   * Methods to update the player's data.
   */

  fun updateRotation(x: Double, y: Double, z: Double) {
    rotation[0] = x
    rotation[1] = y
    rotation[2] = z
  }

  fun updatePosition(x: Double, y: Double, z: Double) {
    position[0] = x
    position[1] = y
    position[2] = z
  }

  fun updateHealth(health: Int) {
    this.health = health
  }

  fun updateScore(score: Int) {
    this.score = score
  }


  /**
   * Getters
   */

  fun getUsername(): String {
    return username
  }

  fun getScore(): Int {
    return score
  }

  fun getHealth(): Int {
    return health
  }

  fun getRotation(): DoubleArray {
    return rotation
  }

  fun getPosition(): DoubleArray {
    return position
  }

  fun getInitialRotation(): DoubleArray {
    return initialRotation
  }
}
