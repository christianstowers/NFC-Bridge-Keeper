package com.example.nfcbridgekeeper.viewmodels

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.nfcbridgekeeper.objects.NfcMessage
import java.nio.charset.Charset

class NFCViewModel : ViewModel() {
    var textToSend by mutableStateOf("")
        private set

    var receivedText by mutableStateOf("[ no message received yet ]")
        private set

    /**
     * updates the text to send and sets it in the singleton for HostApduService.
     */
    fun updateTextToSend(newText: String) {
        textToSend = newText
        Log.d("new letter change: %s", newText)
        // update the singleton to reflect the new message
        NfcMessage.messageToSend = newText
    }

    /**
     * Updates the received text.
     */
    fun updateReceivedText(newText: String) {
        receivedText = newText
        Log.d("viewmodel", "text update: $newText")
    }

    /**
     * Handles NFC intents to extract received messages.
     */
    fun handleNfcIntent(intent: Intent) {
        val action = intent.action
        if (action == android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED
        ) {
            val tag: Tag? = intent.getParcelableExtra(android.nfc.NfcAdapter.EXTRA_TAG, Tag::class.java)
//            val tag: Tag? = intent.getParcelableExtra(android.nfc.NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val ndef = Ndef.get(tag)
                if (ndef != null) {
                    try {
                        ndef.connect()
                        val ndefMessage = ndef.ndefMessage
                        if (ndefMessage != null) {
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
                                    updateReceivedText(text)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            ndef.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}