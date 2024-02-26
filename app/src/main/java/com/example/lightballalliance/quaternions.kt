package com.example.lightballalliance

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt


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

// Spherical linear interpolation for two quaternions
fun slerpQuaternions(q1: FloatArray, q2: FloatArray, t: Float): FloatArray {
  var x1 = q1[0]
  var y1 = q1[1]
  var z1 = q1[2]
  var w1 = q1[3]

  val x2 = q2[0]
  val y2 = q2[1]
  val z2 = q2[2]
  val w2 = q2[3]

  // Calculate the dot product
  var dot = x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2

  // If the dot product is negative, the quaternions are more than 90 degrees apart
  // and slerp won't take the shorter path. Fix by reversing one quaternion.
  if (dot < 0) {
    x1 *= -1
    y1 *= -1
    z1 *= -1
    w1 *= -1
    dot *= -1
  }

  // Set the first and second quaternions to be the same
  if (dot > 0.9995) {
    val result = FloatArray(4)
    result[0] = x1 + t * (x2 - x1)
    result[1] = y1 + t * (y2 - y1)
    result[2] = z1 + t * (z2 - z1)
    result[3] = w1 + t * (w2 - w1)
    return result
  }

  // Calculate the angle between the quaternions
  val theta0 = acos(dot)
  val theta = theta0 * t

  // Calculate the new quaternion
  val w3 = w2 - w1 * dot
  val x3 = x2 - x1 * dot
  val y3 = y2 - y1 * dot
  val z3 = z2 - z1 * dot
  val l = sqrt(w3 * w3 + x3 * x3 + y3 * y3 + z3 * z3)
  val result = FloatArray(4)
  result[0] = x1 * cos(theta) + x3 / l * sin(theta)
  result[1] = y1 * cos(theta) + y3 / l * sin(theta)
  result[2] = z1 * cos(theta) + z3 / l * sin(theta)
  result[3] = w1 * cos(theta) + w3 / l * sin(theta)
  return result
}
