package com.example.morsekey

import android.annotation.SuppressLint
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.provider.Telephony
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var webView: WebView
    private var tts: TextToSpeech? = null

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages?.forEach { msg ->
                    val sender = msg.originatingAddress ?: "Unknown"
                    val body = msg.messageBody ?: ""
                    tts?.speak("New message from $sender. $body", TextToSpeech.QUEUE_ADD, null, "SMS_NOTIF")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WAKE LOCK (Keeps screen on)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tts = TextToSpeech(this, this)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(ContactInterface(this, tts), "Android")
        webView.loadUrl("file:///android_asset/index.html")

        registerReceiver(smsReceiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.85f)
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(smsReceiver) } catch (e: Exception) {}
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    fun sendContactsToWeb(jsonContacts: String) {
        runOnUiThread {
            val safeJson = jsonContacts.replace("'", "\\'")
            webView.evaluateJavascript("displayContacts('$safeJson')", null)
        }
    }
}

class ContactInterface(private val activity: MainActivity, private val tts: TextToSpeech?) {

    // --- DEMO MODE SWITCH ---
    // Set to TRUE for screen recording (Uses fake contacts)
    private val IS_DEMO_MODE = false

    @JavascriptInterface
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    @JavascriptInterface
    fun getLastMessage(phoneNumber: String): String {
        if (IS_DEMO_MODE) {
            return "Hey! This is a test message for the demo video. The app is working great!"
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return ""
        }
        try {
            val targetNum = phoneNumber.replace("[^0-9]".toRegex(), "")
            if (targetNum.isEmpty()) return ""
            val cursor = activity.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body"),
                null, null, "date DESC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(0)?.replace("[^0-9]".toRegex(), "") ?: ""
                    val body = it.getString(1)
                    if (address.isNotEmpty() && (address.endsWith(targetNum) || targetNum.endsWith(address))) {
                        return body ?: ""
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return "No recent messages."
    }

    @JavascriptInterface
    fun requestContacts() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.CALL_PHONE
        )
        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(activity, permissions, 1)
        } else {
            Thread {
                val contactsJson = getPhoneContacts()
                activity.sendContactsToWeb(contactsJson)
            }.start()
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    @JavascriptInterface
    fun makeCall(phoneNumber: String) {
        // --- MODIFIED FOR DEMO ---
        // We ALLOW the real call to proceed so the system dialer launches.
        // This effectively demonstrates the CALL_PHONE permission.

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = Uri.parse("tel:$phoneNumber")
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(activity, "Call Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(activity, "Call Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun sendText(phoneNumber: String, messageBody: String) {
        // We KEEP the restriction on texts to avoid spamming real networks with fake numbers.
        // The visual feedback in the app (the "SENT" screen) is sufficient proof for video.
        if (IS_DEMO_MODE) {
            Toast.makeText(activity, "DEMO: Message Sent", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, messageBody, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun vibrate(milliseconds: Long) {
        val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    @SuppressLint("Range")
    private fun getPhoneContacts(): String {
        if (IS_DEMO_MODE) {
            val demoContacts = JSONArray()

            fun addDemoContact(name: String, number: String) {
                val contact = JSONObject()
                contact.put("name", name)
                val numbers = JSONArray()
                val numObj = JSONObject()
                numObj.put("number", number)
                numObj.put("label", "MOBILE")
                numbers.put(numObj)
                contact.put("numbers", numbers)
                demoContacts.put(contact)
            }

            // Note: 555-XXXX numbers are standard "fictional" numbers in the US/Canada.
            // Calling them results in a harmless carrier message.
            addDemoContact("Alice Smith", "555-0101")
            addDemoContact("Bob Johnson", "555-0102")
            addDemoContact("Charlie Brown", "555-0103")
            addDemoContact("Dad", "555-0104")
            addDemoContact("Emergency", "911")
            addDemoContact("Jane Doe", "555-0105")
            addDemoContact("Mom", "555-0106")
            addDemoContact("Support", "1-800-555-0199")
            addDemoContact("Work", "555-0109")

            return demoContacts.toString()
        }

        val contactsMap = LinkedHashMap<String, JSONObject>()
        try {
            val cursor: Cursor? = activity.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: continue
                    val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: continue
                    val type = it.getInt(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))

                    if (type != ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) continue

                    val customLabel = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL))
                    val normalizedNumber = number.replace("[^0-9+]".toRegex(), "")
                    if (number.contains("@")) continue

                    if (!contactsMap.containsKey(name)) {
                        val contactObj = JSONObject()
                        contactObj.put("name", name)
                        contactObj.put("numbers", JSONArray())
                        contactsMap[name] = contactObj
                    }

                    val label = "MOBILE"
                    val numberObj = JSONObject()
                    numberObj.put("number", number)
                    numberObj.put("label", label)

                    val existingNumbers = contactsMap[name]!!.getJSONArray("numbers")
                    var isDuplicate = false
                    for (i in 0 until existingNumbers.length()) {
                        val existingNum = existingNumbers.getJSONObject(i).getString("number").replace("[^0-9+]".toRegex(), "")
                        if (existingNum == normalizedNumber) { isDuplicate = true; break }
                    }
                    if (!isDuplicate) existingNumbers.put(numberObj)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        val finalArray = JSONArray()
        contactsMap.values.forEach { finalArray.put(it) }
        return finalArray.toString()
    }
}

