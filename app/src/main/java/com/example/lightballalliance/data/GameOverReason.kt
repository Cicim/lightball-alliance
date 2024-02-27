package com.example.lightballalliance.data

sealed class GameOverReason {
  data class Won(val username: String) : GameOverReason()
  data object Tied : GameOverReason()
  data class Disconnect(val username: String) : GameOverReason()
  data class Died(val username: String) : GameOverReason()
}
