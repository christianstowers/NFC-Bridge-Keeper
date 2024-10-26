package com.example.nfcbridgekeeper.services

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.nfcbridgekeeper.objects.NfcMessage

class MyHostApduService : HostApduService() {

    companion object {
        private const val TAG = "MyHostApduService"

        // Define APDU command headers
        private const val SELECT_APDU_HEADER = "00A40400"
        private const val SELECT_OK_SW = "9000"
        private const val UNKNOWN_CMD_SW = "0000"
    }

    /**
     * processAdpuCommand:
     *  - Listens for incoming APDU commands.
     *  - Checks if the command is a SELECT AID command by comparing the APDU header.
     *  - If it is a SELECT command, responds with an NDEF message containing the string to send,
     *      followed by the status word 9000 indicating success.
     *  - For unknown commands, responds with 0000.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        if (commandApdu == null) {
            Log.e(TAG, "Received null APDU command")
            return hexStringToByteArray(UNKNOWN_CMD_SW)
        }

        val commandHex = byteArrayToHexString(commandApdu)
        Log.d(TAG, "Received APDU command: $commandHex")

        // Check if the command is a SELECT AID command
        if (commandHex.startsWith(SELECT_APDU_HEADER)) {
            // Respond with the NDEF message
            val ndefMessage = createNdefMessage(NfcMessage.messageToSend)
            return ndefMessage + hexStringToByteArray(SELECT_OK_SW)
        }

        // Unknown command
        return hexStringToByteArray(UNKNOWN_CMD_SW)
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
    }

    /**
     * createNdefMessage:
     *  - Constructs an NDEF message containing a single text record with the specified string.
     *  - Follows the NDEF text record format, including language code and UTF-8 encoded text.
     */
    private fun createNdefMessage(text: String): ByteArray {
        val language = "en"
        val langBytes = language.toByteArray(Charsets.US_ASCII)
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + langBytes.size + textBytes.size)

        // Status byte: 8-bit
        payload[0] = langBytes.size.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        // Create NDEF Record
        val ndefRecord = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )

        // Create NDEF Message
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))
        return ndefMessage.toByteArray()
    }

    // Utility functions to convert between byte arrays and hex strings
    /** byteArrayToHexString: Converts a byte array to a hexadecimal string representation. */
    private fun byteArrayToHexString(arr: ByteArray): String {
        return arr.joinToString("") { String.format("%02X", it) }
    }

    /**hexStringToByteArray: Converts a hexadecimal string back to a byte array. */
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