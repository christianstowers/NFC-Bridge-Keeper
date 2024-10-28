package com.example.nfcbridgekeeper.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeKeeperUI(
    textToSend: String,
    onTextChange: (String) -> Unit,
    receivedText: String
) {
    /**
     * The version of the app on the feature/initial-build branch is the HCE Card actor. Note that
     * the onscreen titles differ to aid in distinguishing the tools.
     */
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NFC Bridge Keeper")})
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "NFC Reader Tool",
                    style = MaterialTheme.typography.headlineMedium
                )
//                Spacer(modifier = Modifier.height(8.dp))
//                TextField(
//                    value = textToSend,
//                    onValueChange = onTextChange,
//                    label = { Text("Enter text to send") },
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Received Text:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = receivedText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Hold your device near another NFC-enabled device running the HCE " +
                            "Card Tool version of the app to receive the message.",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    )
}