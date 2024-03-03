package com.example.lightballalliance

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightballalliance.ui.theme.lightballallianceTheme
import java.io.IOException


@Composable
fun MainScreen(mainViewModel: MainViewModel = viewModel()) {
  val state by mainViewModel.uiState.collectAsState()

  lightballallianceTheme {
    // A surface container using the 'background' color from the theme
    Surface(color = MaterialTheme.colorScheme.background) {
      Row (
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        Logo()
      }

      Column (
        Modifier
          .fillMaxSize()
          .padding(top = 200.dp),
        verticalArrangement = Arrangement.Center
      ) {
        AddressTextBox(
          address = state.address,
          onAddressChange = { mainViewModel.setAddress(it) },
          isConnected = state.isConnected,
          connectionError = state.connectionError
        )

        NameTextBox(
          askName = WebSocketClient.playerName.value,
          onNameChange = { WebSocketClient.playerName.value = it },
          isConnected = state.isConnected,
          confirmName = { mainViewModel.confirmName() },
          nameAlreadyTaken = state.nameAlreadyTaken
        )

        ConnectButtons(
          onClickConnect = { mainViewModel.tryConnecting() },
          onClickDisconnect = { WebSocketClient.disconnect() },
          isConnected = state.isConnected,
          isConnecting = state.isConnecting
        )
      }
    }
  }
}


@Composable
fun AddressTextBox(address: String, onAddressChange: (String) -> Unit, isConnected: Boolean, connectionError: Boolean) {
  // Create composable text box to enter the server address.
  Column (
    Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    OutlinedTextField(
      value = address,
      onValueChange = onAddressChange,
      label = { Text("Server Address (ws://host:port/path)") },
      enabled = !isConnected,
      isError = connectionError
    )

    if (connectionError) {
      Text(
        text = "Cannot connect to the server.\nPlease check the address and try again.",
        color = Color.Red,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(10.dp),
      )
    }
  }
}

@Composable
fun NameTextBox(
  askName: String,
  onNameChange: (String) -> Unit,
  isConnected: Boolean, confirmName: () -> Unit,
  nameAlreadyTaken: Boolean
) {
  // Create composable text box to enter the name.
  if (askName != "UnUsAb13_Us3Rn4M3" && isConnected) {
    Column (
      Modifier.padding(10.dp)
    ) {
      Text(
        text = "Welcome to the game!\nChoose your name:",
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )

      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        OutlinedTextField(
          value = askName,
          onValueChange = onNameChange,
          label = { Text("Name") },
          isError = nameAlreadyTaken
        )
      }

      if (nameAlreadyTaken) {
        Text(
          text = "This name is already taken.\nPlease choose another one.",
          color = Color.Red,
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(10.dp),
        )
      }

      Button(
        onClick = confirmName,
        modifier = Modifier
          .padding(bottom = 10.dp, top = 5.dp)
          .align(Alignment.CenterHorizontally)
      ) {
        Text(text = "Confirm")
      }
    }
  }
}

@Composable
fun ConnectButtons(
  onClickConnect: () -> Unit,
  onClickDisconnect: () -> Unit,
  isConnected: Boolean,
  isConnecting: Boolean
) {
  Row (
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    Button(
      onClick = onClickConnect,
      enabled = !isConnected && !isConnecting,
      modifier = Modifier.padding(2.dp)
    ) {
      if (isConnecting)
        Text(text = "Connecting...")
      else
        Text(text = "Connect")
    }

    Button(
      onClick = onClickDisconnect,
      enabled = isConnected,
      modifier = Modifier.padding(2.dp)
    ) {
      Text(text = "Disconnect")
    }
  }
}

@Composable
fun Logo() {
  // Load the image from the assets folder
  val context = LocalContext.current
  val bitmap = loadImageResource(context, "extendedLogo.png").resource.asAndroidBitmap()

  // Display the loaded bitmap
  Image(
    bitmap = bitmap.asImageBitmap(),
    contentDescription = "Lightball Alliance Logo",
    alignment = Alignment.TopCenter,
    modifier = Modifier
      .padding(top = 20.dp)
      .height(250.dp)
      .width(250.dp)
  )
}

@Composable
fun loadImageResource(context: Context, path: String): ImageAsset {
  return runCatching {
    val inputStream = context.assets.open(path)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    ImageAsset(bitmap.asImageBitmap())
  }.getOrElse {
    throw IOException("Could not load image from assets at $path")
  }
}

class ImageAsset(val resource: androidx.compose.ui.graphics.ImageBitmap)
