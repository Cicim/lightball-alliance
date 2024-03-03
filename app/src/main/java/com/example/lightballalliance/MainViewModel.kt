package com.example.lightballalliance

import androidx.lifecycle.ViewModel
import com.example.lightballalliance.data.ClientMessage
import com.example.lightballalliance.data.sendClientMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val DEFAULT_SERVER = "ws://10.0.2.2:8080"

data class MainState(
  val address: String = DEFAULT_SERVER,
  val isConnected: Boolean = false,
  val isConnecting: Boolean = false,
  val nameConfirmed: Boolean = false,
  val nameAlreadyTaken: Boolean = false,
  val connectionError: Boolean = false,
)

class MainViewModel : ViewModel() {
  // Expose UI state as observable
  private val _uiState = MutableStateFlow(MainState())
  val uiState: StateFlow<MainState> = _uiState.asStateFlow()

  // Business logic goes here.
  fun setAddress(address: String) {
    _uiState.value = _uiState.value.copy(address = address)
  }

  fun confirmName() {
    _uiState.value = _uiState.value.copy(nameConfirmed = true)
    sendClientMessage(ClientMessage.Name(WebSocketClient.playerName.value))
  }

  fun tryConnecting() {
    _uiState.value = _uiState.value.copy(isConnecting = true, connectionError = false)
    WebSocketClient.connect(_uiState.value.address)
  }

  fun setConnected() {
    _uiState.value = _uiState.value.copy(isConnected = true, isConnecting = false)
  }

  fun askNameAgain() {
    if (_uiState.value.nameConfirmed) {
      _uiState.value = _uiState.value.copy(nameConfirmed = false, nameAlreadyTaken = true)
    }
  }

  fun setDisconnected() {
    _uiState.value = _uiState.value.copy(isConnected = false, isConnecting = false)
  }

  fun setConnectionError() {
    _uiState.value = _uiState.value.copy(connectionError = true)
  }

  fun clearState() {
    // Only keep the address
    _uiState.value = MainState(address = _uiState.value.address)
  }
}
