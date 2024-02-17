package com.example.lightballalliance

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Orientation(
  val x: Double = 0.0,
  val y: Double = 0.0,
  val z: Double = 0.0
)

class OrientationViewModel : ViewModel() {
  private val _orientation = MutableStateFlow(Orientation())
  val orientation: StateFlow<Orientation> = _orientation.asStateFlow()

  fun updateOrientation(x: Double, y: Double, z: Double) {
    _orientation.update { orientation ->
      orientation.copy(
        x = x,
        y = y,
        z = z
      )}
  }
}
