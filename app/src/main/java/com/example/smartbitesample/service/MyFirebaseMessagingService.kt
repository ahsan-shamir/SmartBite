package com.example.smartbitesample.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smartbitesample.MainActivity
import com.example.smartbitesample.NotificationEntity
import com.example.smartbitesample.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: remoteMessage.data["notification_title"]
            ?: "SmartBite"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["notification_message"]
            ?: ""

        if (title.isNotEmpty() || body.isNotEmpty()) {
            saveToDatabase(title, body)
            sendNotification(title, body)
        }
    }

    private fun saveToDatabase(title: String, message: String) {
        val prefs = getSharedPreferences("smart_bite_prefs", Context.MODE_PRIVATE)
        val cachedUsername = prefs.getString("last_logged_in_username", null)

        if (cachedUsername != null) {
            performFirestoreSave(cachedUsername, title, message)
        } else {
            val email = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
            if (email != null) {
                FirebaseFirestore.getInstance().collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { query ->
                        val username = query.documents.firstOrNull()?.id
                        if (username != null) performFirestoreSave(username, title, message)
                        else Log.w("SmartBite_FCM", "Could not map email to username")
                    }
                    .addOnFailureListener { e -> Log.e("SmartBite_FCM", "Firestore lookup error", e) }
            } else {
                Log.w("SmartBite_FCM", "No user context available — cannot save notification")
            }
        }
    }

    private fun performFirestoreSave(username: String, title: String, message: String) {
        val firestore = FirebaseFirestore.getInstance()
        val notifRef = firestore.collection("users").document(username)
            .collection("notifications").document()

        val notification = NotificationEntity(
            id = notifRef.id,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        notifRef.set(notification)
            .addOnSuccessListener { Log.d("SmartBite_FCM", "Notification saved for: $username") }
            .addOnFailureListener { e -> Log.e("SmartBite_FCM", "Notification save failed", e) }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("notification_title", title)
            putExtra("notification_message", messageBody)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "smart_bite_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "SmartBite Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Food recommendations and order updates"
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("SmartBite_FCM", "FCM token refreshed: $token")
    }
}
