package com.example.nfcbridgekeeper

import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.nfcbridgekeeper.ui.theme.NFCBridgeKeeperTheme
import android.content.Intent
import android.nfc.NfcAdapter
import java.nio.charset.Charset
import com.example.nfcbridgekeeper.viewmodels.NFCViewModel
import android.app.PendingIntent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.util.Log
import com.example.nfcbridgekeeper.ui.theme.BridgeKeeperUI

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    private val viewModel: NFCViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NFC Adapter initialization
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Log.d("MainActivity", "NFC is not available on this device.")
            finish()
            return
        }

        // Creates a generic PendingIntent that will be delivered to this activity.
        // The NFC stack will fill in the intent with the details of the discovered tag.
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // todo: no need for filters in reader mode. the intent filters are present in the hce
        //  card tool. filters have been removed for brevity.

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
    }

    override fun onResume() {
        super.onResume()
        // Enable Reader Mode
        nfcAdapter?.enableReaderMode(
            this,
            { tag ->
                // todo: handle detected tag
                val isoDep = IsoDep.get(tag)
                isoDep?.let {
                    try {
                        it.connect()
                        Log.d("MainActivity", "connected to IsoDep tag.")

                        // todo: construct and send SELECT AID command, expect 9000 response
                        val selectApdu = buildSelectApdu("F0010203040507")
                        Log.d("MainActivity", "sending SELECT APDU: ${byteArrayToHex(selectApdu)}")
                        val result = it.transceive(selectApdu)
                        val response = byteArrayToHex(result)
                        Log.d("MainActivity", "APDU Response: $response")

                        if (response.contains("9000")) {
//                            // todo: get NDEF message from response
//                            val ndefBytes = result.copyOfRange(0, result.size - 2)
//                            val ndefMessage = String(ndefBytes, Charset.forName("UTF-8"))
//                            Log.d("MainActivity", "received NDEF Message: $ndefMessage")
                            // Extract NDEF message from response
                            val ndefBytes = result.copyOfRange(0, result.size - 2) // Remove status word
                            val ndefMessage = parseNdefMessage(ndefBytes)
                            Log.d("MainActivity", "Received NDEF Message: $ndefMessage")
                            // todo: update received text
                            viewModel.updateReceivedText(ndefMessage)
                        } else {
                            Log.e("MainActivity", "failed to select AID.")
                        }

                    } catch (e: Exception) {
                        Log.e("MainActivity", "error communicating with tag: ${e.message}")
                    } finally {
                        try {
                            it.close()
                            Log.d("MainActivity", "IsoDep connection closed.")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "error closing IsoDep connection: ${e.message}")
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

    private fun parseNdefMessage(ndefBytes: ByteArray): String {
        return try {
            val ndefMessage = NdefMessage(ndefBytes)
            if (ndefMessage.records.isNotEmpty()) {
                val record = ndefMessage.records[0]
                if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    val payload = record.payload
                    val languageCodeLength = payload[0].toInt() and 0x3F
                    String(
                        payload,
                        1 + languageCodeLength,
                        payload.size - 1 - languageCodeLength,
                        Charset.forName("UTF-8")
                    )
                } else {
                    Log.e("MainActivity", "Unsupported NDEF record type.")
                    "Unsupported record type."
                }
            } else {
                Log.e("MainActivity", "No NDEF records found.")
                "No records found."
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error parsing NDEF message: ${e.message}")
            "Error parsing message."
        }
    }
}