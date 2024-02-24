package com.example.lightballalliance.data

class Enemy (
  // Constructor
  private var id: Int,
  private var health: Int,
  hexColor: Int,
  private var startTime: Int,
  private var speed: Float,
  private var source: FloatArray,
  private var target: FloatArray
) {
  private var color: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

  /**
   * Methods to update the enemy's data.
   */

  init {
    setColor(hexColor)
  }

  fun updateHealth(health: Int) {
    this.health = health
  }

  fun setColor(hexColor: Int) {
    val r = ((hexColor shr 16) and 0xFF) / 255.0f
    val g = ((hexColor shr 8) and 0xFF) / 255.0f
    val b = (hexColor and 0xFF) / 255.0f
    color = floatArrayOf(r, g, b, 1.0f)
  }

  /**
   * Getters
   */

  fun getHealth(): Int {
    return health
  }

  fun getColor(): FloatArray {
    return color
  }

  fun getId(): Int {
    return id
  }

  // Return the current position of the enemy.
  fun getPosition(time: Int): FloatArray {
    val x = source[0] + (target[0] - source[0]) * (time - startTime) * speed
    val y = source[1] + (target[1] - source[1]) * (time - startTime) * speed
    val z = source[2] + (target[2] - source[2]) * (time - startTime) * speed
    return floatArrayOf(x, y, z)
  }
}
