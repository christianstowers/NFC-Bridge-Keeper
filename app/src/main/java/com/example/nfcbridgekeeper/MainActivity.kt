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
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.util.Log
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
            Log.d("MainActivity", "NFC is not available on this device.")
            finish()
            return
        }

        // Create a generic PendingIntent that will be delivered to this activity.
        // The NFC stack will fill in the intent with the details of the discovered tag.
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // no need for filters in reader mode. the intent filters are present in the hce card tool.

        intentFiltersArray = arrayOf()

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

        //todo: disabled WIP, onResume approach using isoDep
        // Setup Reader Mode
//        nfcAdapter?.enableReaderMode(
//            this,
//            { tag ->
//                // Handle the tag detected
//                val ndef = Ndef.get(tag)
//                ndef?.let {
//                    try {
//                        it.connect()
//                        val message = it.ndefMessage
//                        message?.let { ndefMessage ->
//                            val records = ndefMessage.records
//                            for (record in records) {
//                                if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
//                                    record.type.contentEquals(NdefRecord.RTD_TEXT)
//                                ) {
//                                    val payload = record.payload
//                                    val languageCodeLength = payload[0].toInt() and 0x3F
//                                    val text = String(
//                                        payload,
//                                        1 + languageCodeLength,
//                                        payload.size - 1 - languageCodeLength,
//                                        Charset.forName("UTF-8")
//                                    )
//                                    // Update received text
//                                    Log.d("mainactivity", "updated text in activity: $text")
//                                    viewModel.updateReceivedText(text)
//                                }
//                            }
//                        }
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    } finally {
//                        try {
//                            it.close()
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                    }
//                }
//            },
//            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
//            null
//        )
    }

    override fun onResume() {
        super.onResume()
        // Enable Reader Mode
        nfcAdapter?.enableReaderMode(
            this,
            { tag ->
                // Handle the tag detected
                val isoDep = IsoDep.get(tag)
                isoDep?.let {
                    try {
                        it.connect()
                        Log.d("MainActivity", "Connected to IsoDep tag.")

                        // Construct SELECT AID command
                        val selectApdu = buildSelectApdu("F0010203040507")
                        Log.d("MainActivity", "Sending SELECT APDU: ${byteArrayToHex(selectApdu)}")
                        // Send SELECT AID command
                        val result = it.transceive(selectApdu)
                        val response = byteArrayToHex(result)
                        Log.d("MainActivity", "APDU Response: $response")

                        if (response.contains("9000")) { // Check for success status word
                            // Extract NDEF message from response
                            val ndefBytes = result.copyOfRange(0, result.size - 2)
                            val ndefMessage = String(ndefBytes, Charset.forName("UTF-8"))
                            Log.d("MainActivity", "Received NDEF Message: $ndefMessage")
                            // Update received text in ViewModel
                            viewModel.updateReceivedText(ndefMessage)
                        } else {
                            Log.e("MainActivity", "Failed to select AID.")
                        }

                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error communicating with tag: ${e.message}")
                    } finally {
                        try {
                            it.close()
                            Log.d("MainActivity", "IsoDep connection closed.")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error closing IsoDep connection: ${e.message}")
                        }
                    }
                } ?: run {
                    Log.e("MainActivity", "IsoDep not supported by this tag.")
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle NFC intent
        //todo: not needed for reader mode
//        viewModel.handleNfcIntent(intent)
    }

    private fun buildSelectApdu(aid: String): ByteArray {
        return hexStringToByteArray("00A40400" + String.format("%02X", aid.length / 2) + aid)
    }

    private fun byteArrayToHex(arr: ByteArray): String {
        return arr.joinToString("") { String.format("%02X", it) }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }
}