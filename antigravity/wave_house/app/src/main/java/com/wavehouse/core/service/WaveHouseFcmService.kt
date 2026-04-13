package com.wavehouse.core.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Firebase Cloud Messaging service để nhận push notification.
 * Dùng cho: cảnh báo tồn kho thấp, thông báo đơn hàng mới.
 */
class WaveHouseFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token mới: $token")
        // TODO: Gửi token lên Firestore /users/{uid}/fcmTokens
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM nhận: ${remoteMessage.notification?.title}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: return
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val type = remoteMessage.data["type"] ?: "GENERAL"

        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, type: String) {
        // TODO: Implement Android Notification via NotificationManager
        // Channel: LOW_STOCK_ALERT, ORDER_UPDATE, GENERAL
        Timber.d("Notification [$type]: $title — $body")
    }
}
