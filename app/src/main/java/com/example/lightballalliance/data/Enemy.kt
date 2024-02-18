package com.example.lightballalliance.data

class Enemy (
  // Constructor
  private var id: Int,
  private var health: Int,
  private var color: Int,
  private var startTime: Int,
  private var speed: Double,
  private var source: DoubleArray,
  private var target: DoubleArray
) {

  /**
   * Methods to update the enemy's data.
   */

  fun updateHealth(health: Int) {
    this.health = health
  }

  /**
   * Getters
   */

  fun getHealth(): Int {
    return health
  }

  fun getColor(): Int {
    return color
  }

  fun getId(): Int {
    return id
  }

  // Return the current position of the enemy.
  fun getPosition(): DoubleArray {
    val x = source[0] + (target[0] - source[0]) * (System.currentTimeMillis() - startTime) * speed
    val y = source[1] + (target[1] - source[1]) * (System.currentTimeMillis() - startTime) * speed
    val z = source[2] + (target[2] - source[2]) * (System.currentTimeMillis() - startTime) * speed
    return doubleArrayOf(x, y, z)
  }
}
