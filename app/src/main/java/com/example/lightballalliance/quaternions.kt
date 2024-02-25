package com.example.lightballalliance

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin


// Function to convert Euler angles to a quaternion
fun eulerAnglesToQuaternion(roll: Double, pitch: Double, yaw: Double): FloatArray {
  val cy = cos(yaw * 0.5).toFloat()
  val sy = sin(yaw * 0.5).toFloat()
  val cp = cos(pitch * 0.5).toFloat()
  val sp = sin(pitch * 0.5).toFloat()
  val cr = cos(roll * 0.5).toFloat()
  val sr = sin(roll * 0.5).toFloat()

  val w = cr * cp * cy + sr * sp * sy
  val x = sr * cp * cy - cr * sp * sy
  val y = cr * sp * cy + sr * cp * sy
  val z = cr * cp * sy - sr * sp * cy

  return floatArrayOf(x, y, z, w)
}

// Function to multiply two quaternions
fun multiplyQuaternions(q1: FloatArray, q2: FloatArray): FloatArray {
  val x = q1[3] * q2[0] + q1[0] * q2[3] + q1[1] * q2[2] - q1[2] * q2[1]
  val y = q1[3] * q2[1] - q1[0] * q2[2] + q1[1] * q2[3] + q1[2] * q2[0]
  val z = q1[3] * q2[2] + q1[0] * q2[1] - q1[1] * q2[0] + q1[2] * q2[3]
  val w = q1[3] * q2[3] - q1[0] * q2[0] - q1[1] * q2[1] - q1[2] * q2[2]

  return floatArrayOf(x, y, z, w)
}

// Function to convert a quaternion to Euler angles (roll, pitch, and yaw)
fun quaternionToEulerAngles(v: FloatArray): DoubleArray {
  val qx = v[0].toDouble()
  val qy = v[1].toDouble()
  val qz = v[2].toDouble()
  val qw = v[3].toDouble()

  val sinr_cosp = 2 * (qw * qx + qy * qz)
  val cosr_cosp = 1 - 2 * (qx * qx + qy * qy)
  val roll = atan2(sinr_cosp, cosr_cosp)

  // pitch / y
  val sinp = 2 * (qw * qy - qz * qx)
  val pitch = if (abs(sinp) >= 1) {
    Math.PI / 2 * sign(sinp)
  } else {
    asin(sinp)
  }

  // yaw / z
  val siny_cosp = 2 * (qw * qz + qx * qy)
  val cosy_cosp = 1 - 2 * (qy * qy + qz * qz)
  val yaw = atan2(siny_cosp, cosy_cosp)

  return doubleArrayOf(roll, pitch, yaw)
}