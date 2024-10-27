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
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.nfcbridgekeeper.ui.theme.BridgeKeeperUI

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

        // Setup Reader Mode
        nfcAdapter?.enableReaderMode(
            this,
            { tag ->
                // Handle the tag detected
                val ndef = Ndef.get(tag)
                ndef?.let {
                    try {
                        it.connect()
                        val message = it.ndefMessage
                        message?.let { ndefMessage ->
                            val records = ndefMessage.records
                            for (record in records) {
                                if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                                    record.type.contentEquals(NdefRecord.RTD_TEXT)
                                ) {
                                    val payload = record.payload
                                    val languageCodeLength = payload[0].toInt() and 0x3F
                                    val text = String(
                                        payload,
                                        1 + languageCodeLength,
                                        payload.size - 1 - languageCodeLength,
                                        Charset.forName("UTF-8")
                                    )
                                    // Update received text
                                    viewModel.updateReceivedText(text)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            it.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle NFC intent
        viewModel.handleNfcIntent(intent)
    }
}