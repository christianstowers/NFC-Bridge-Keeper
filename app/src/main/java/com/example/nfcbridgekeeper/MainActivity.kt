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



//class MainActivity : ComponentActivity() {
//
//    private var nfcAdapter: NfcAdapter? = null
//    private var ndefPushMessage: NdefMessage? = null
//    private val viewModel: NFCViewModel by viewModels()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Initialize NFC Adapter
//        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
//
//        if (nfcAdapter == null) {
//            // NFC is not available on this device
//            // Optionally, inform the user and close the app
//        }
//
//        setContent {
//            var textToSend by remember { mutableStateOf("") }
//
//            NFCBridgeKeeperTheme {
//                // Create and set the NDEF Push Message
//                ndefPushMessage = if (textToSend.isNotEmpty()) {
//                    createNdefMessage(textToSend)
//                } else {
//                    null
//                }
//
//                nfcAdapter?.set(ndefPushMessage, this)
//
//                // UI Components
//                NFCAppUI(
//                    textToSend = textToSend,
//                    onTextChange = { textToSend = it },
//                    receivedText = viewModel.receivedText
//                )
//            }
//        }
//    }
//
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
//            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
//            if (rawMessages != null) {
//                val messages = rawMessages.map { it as NdefMessage }
//                for (message in messages) {
//                    for (record in message.records) {
//                        if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
//                            record.type.contentEquals(NdefRecord.RTD_TEXT)
//                        ) {
//                            val payload = record.payload
//                            val languageCodeLength = payload[0].toInt() and 0x3F
//                            val text = String(
//                                payload,
//                                1 + languageCodeLength,
//                                payload.size - 1 - languageCodeLength,
//                                Charset.forName("UTF-8")
//                            )
//                            viewModel.updateReceivedText(text)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun createNdefMessage(text: String): NdefMessage {
//        val language = "en"
//        val langBytes = language.toByteArray(Charsets.US_ASCII)
//        val textBytes = text.toByteArray(Charsets.UTF_8)
//        val payload = ByteArray(1 + langBytes.size + textBytes.size)
//
//        // Status byte: 8-bit
//        payload[0] = langBytes.size.toByte()
//        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
//        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
//
//        val textRecord = NdefRecord(
//            NdefRecord.TNF_WELL_KNOWN,
//            NdefRecord.RTD_TEXT,
//            ByteArray(0),
//            payload
//        )
//
//        return NdefMessage(arrayOf(textRecord))
//    }
//}


///**
// * Notes:
// * - NfcAdapter Initialization: Checks if NFC is available and enabled. If not, prompts the user to
// * enable NFC.
// *
// * - Lifecycle Methods: onResume and onPause handle enabling and disabling foreground dispatch to
// * ensure the app can handle NFC intents when in the foreground.
// *
// * - Intent Handling: onNewIntent is overridden to handle incoming NFC intents. We'll delegate the
// * processing to the composable.
// */

//class MainActivity : ComponentActivity() {
//
//    private var nfcAdapter: NfcAdapter? = null
//
//    // todo: using mutable state to hold the text to be sent. wip for removing the
//    //  deprecated setNdefPushMessage usage
//    private var textToSend: String by mutableStateOf("")
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // nfc adapter init
//        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
//
//        if (nfcAdapter == null) {
//            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show()
//            finish()
//            return
//        }
//
//        if(!nfcAdapter!!.isEnabled) {
//            Toast.makeText(this, "Please enable NFC.", Toast.LENGTH_LONG).show()
//            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
//        }
//
//        //todo: set NDEF message callback
//        //todo: setNdefPushMessageCallback is an Android Beam feature and is dead boo hiss.
//        // working on a replacement implementation now...
////        nfcAdapter?.setNdefPushMessageCallback(this, this)
//
//        enableEdgeToEdge()  //todo: is this needed?
//        setContent()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Enable foreground dispatch when the app is in the foreground
//        (currentFocus?.context as? ComponentActivity)?.let { activity ->
//            (activity as MainActivity).enableForegroundDispatch()
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        disableForegroundDispatch()
//    }
//
//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//
//        setIntent(intent)
//        setContent()
//    }
//
//    internal fun sendNfcMessage(text: String) {
//        val message = NdefMessage(
//            arrayOf(
//                NdefRecord.createMime("text/plain", text.toByteArray(Charset.forName("UTF-8")))
//            )
//        )
//        nfcAdapter?.setNdefPushMessage(message, this)
//        Toast.makeText(this, "Ready to send NFC message.", Toast.LENGTH_SHORT).show()
//    }
//
//    internal fun receiveNfcMessage(intent: Intent): String? {
//        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
//            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
//            if (rawMessages != null) {
//                val messages = rawMessages.map { it as NdefMessage }
//                for (message in messages) {
//                    for (record in message.records) {
//                        if (record.tnf == NdefRecord.TNF_MIME_MEDIA &&
//                            String(record.type) == "text/plain") {
//                            return String(record.payload, Charset.forName("UTF-8"))
//                        }
//                    }
//                }
//            }
//        }
//        return null
//    }
//
//    private fun enableForegroundDispatch() {
//        val intent = Intent(this, javaClass).apply {
//            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//        }
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_MUTABLE
//        )
//        val filters = arrayOf(
//            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
//                try {
//                    addDataType("text/plain")
//                } catch (e: IntentFilter.MalformedMimeTypeException) {
//                    throw RuntimeException("Failed to add MIME type.", e)
//                }
//            }
//        )
//        val techList = arrayOf(arrayOf(android.nfc.tech.Ndef::class.java.name))
//        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techList)
//    }
//
//    private fun disableForegroundDispatch() {
//        nfcAdapter?.disableForegroundDispatch(this)
//    }
//
//    private fun setContent() {
//        setContent {
//            NFCBridgeKeeperTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    NFCApp(nfcAdapter = nfcAdapter)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun NFCApp(nfcAdapter: NfcAdapter?) {
//    var textToSend by remember { mutableStateOf("") }
//    var receivedText by remember { mutableStateOf("Received text will appear here") }
//
//    val context = LocalContext.current
//
//    // Handle incoming NFC intents
//    LaunchedEffect(Unit) {
//        val activity = context as? ComponentActivity
//        activity?.intent?.let { intent ->
//            val received = (activity as MainActivity).receiveNfcMessage(intent)
//            if (received != null) {
//                receivedText = received
//                Toast.makeText(context, "Received: $received", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    Scaffold(
////        topBar = {
////            TopAppBar(
////                title = { Text("NFC String Transfer") }
////            )
////        },
//        content = { padding ->
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .padding(16.dp),
//                verticalArrangement = Arrangement.Top
//            ) {
//                OutlinedTextField(
//                    value = textToSend,
//                    onValueChange = { textToSend = it },
//                    label = { Text("Enter text to send") },
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Button(
//                    onClick = {
//                        if (textToSend.isNotEmpty()) {
//                            (context as? MainActivity)?.sendNfcMessage(textToSend)
//                        } else {
//                            Toast.makeText(context, "Please enter text to send.", Toast.LENGTH_SHORT).show()
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text("Send via NFC")
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                Text(
//                    text = "Received Text:",
//                    style = MaterialTheme.typography.bodySmall
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                Text(
//                    text = receivedText,
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//        }
//    )
//}
//
