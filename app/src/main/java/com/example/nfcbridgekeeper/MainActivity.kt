package com.example.nfcbridgekeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nfcbridgekeeper.ui.theme.NFCBridgeKeeperTheme
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*

/**
 * Notes:
 * - NfcAdapter Initialization: Checks if NFC is available and enabled. If not, prompts the user to
 * enable NFC.
 *
 * - Lifecycle Methods: onResume and onPause handle enabling and disabling foreground dispatch to
 * ensure the app can handle NFC intents when in the foreground.
 *
 * - Intent Handling: onNewIntent is overridden to handle incoming NFC intents. We'll delegate the
 * processing to the composable.
 */

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // nfc adapter init
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if(!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Please enable NFC.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

        enableEdgeToEdge()  //todo: is this needed?
        setContent {
            NFCBridgeKeeperTheme {
//                Scaffold(
//                    modifier = Modifier.fillMaxSize(),
//                    containerColor = MaterialTheme.colorScheme.) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NFCApp(nfcAdapter = nfcAdapter)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable foreground dispatch when the app is in the foreground
        (currentFocus?.context as? ComponentActivity)?.let { activity ->
            (activity as MainActivity).enableForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //pass intent to the composable
        //todo: will be handled in the NFCApp composable
    }

    private fun enableForegroundDispatch() {
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try {
                    addDataType("text/plain")
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                    throw RuntimeException("Failed to add MIME type.", e)
                }
            }
        )
        val techList = arrayOf(arrayOf(android.nfc.tech.Ndef::class.java.name))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techList)
    }

    private fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NFCBridgeKeeperTheme {
        Greeting("Android")
    }
}