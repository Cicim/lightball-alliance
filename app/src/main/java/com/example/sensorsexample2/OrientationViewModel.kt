package com.example.sensorsexample2

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Orientation(
  val azimuth: Float = 0f,
  val pitch: Float = 0f,
  val roll: Float = 0f
)

class OrientationViewModel : ViewModel() {
  private val _orientation = MutableStateFlow(Orientation())
  val orientation: StateFlow<Orientation> = _orientation.asStateFlow()

  fun updateOrientation(azimuth: Float, pitch: Float, roll: Float) {
    _orientation.update { orientation ->
      orientation.copy(
        azimuth = azimuth,
        pitch = pitch,
        roll = roll
      )}
  }
}
