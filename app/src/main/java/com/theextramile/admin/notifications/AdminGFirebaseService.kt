package com.theextramile.admin.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.theextramile.admin.MainActivity
import com.theextramile.admin.R
import com.theextramile.admin.TEMApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Servicio que recibe notificaciones de Firebase Cloud Messaging.
 * Se invoca automáticamente cuando llega un push del servidor.
 */
class AdminGFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AdminGFCM"
        const val CHANNEL_ID = "admin_g_reservations"
        const val CHANNEL_NAME = "Nuevas reservas"
    }

    /** Llamado cuando se genera un nuevo token de FCM */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        // Mandar el token al servidor
        val app = application as? TEMApplication ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.notificationRepository.registerToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando token", e)
            }
        }
    }

    /** Llamado cuando llega una notificación push */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Notificación recibida: ${message.data}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Nueva reserva"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Tienes una nueva reserva"

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevas reservas recibidas"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent al abrir
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            // Color de marca. Sale de res/values/colors.xml para no repetir
            // el hex: es el color con el que Android tiñe el icono.
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}
