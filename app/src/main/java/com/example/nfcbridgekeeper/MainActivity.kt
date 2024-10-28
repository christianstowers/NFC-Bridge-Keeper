package com.example.nfcbridgekeeper


import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.nfcbridgekeeper.ui.theme.NFCBridgeKeeperTheme
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import java.nio.charset.Charset
import com.example.nfcbridgekeeper.viewmodels.NFCViewModel
import android.app.PendingIntent
import android.content.IntentFilter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.nfcbridgekeeper.ui.theme.BridgeKeeperUI

/**
 * The version of the app on the feature/initial-build branch is the HCE Card actor. For the Reader
 * Tool, checkout and build from feature/reader-mode.
 */

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    private val viewModel: NFCViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            // NFC is not available on this device
            // Optionally, inform the user and disable NFC features
            // For example, show a dialog or a Toast message
            Log.d("MainActivity", "NFC is not available on this device.")
        }

        // Create a generic PendingIntent that will be delivered to this activity.
        // The NFC stack will fill in the intent with the details of the discovered tag.
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // Setup intent filters for NDEF_DISCOVERED
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("text/plain")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Failed to add MIME type.", e)
        }
        intentFiltersArray = arrayOf(ndef)

        // Tech list for Ndef
        techListsArray = arrayOf(arrayOf(Ndef::class.java.name))

        setContent {
            NFCBridgeKeeperTheme {
                BridgeKeeperUI(
                    textToSend = viewModel.textToSend,
                    onTextChange = { viewModel.updateTextToSend(it) },
                    receivedText = viewModel.receivedText
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this, pendingIntent, intentFiltersArray, techListsArray
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle NFC intent
        viewModel.handleNfcIntent(intent)
    }
}