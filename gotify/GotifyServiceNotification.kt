package com.flyingpanda.gotificationtester.gotify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import com.flyingpanda.gotificationtester.R
import java.util.concurrent.ThreadLocalRandom

/**
 * This service is used to receive notifications
 * from gotify (once registered)
 */
var serviceName = "gotify.GotifyServiceNotification"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
open class GotifyServiceNotification : Service() {
    /** For showing and hiding our notification.  */
    private var gNM: NotificationManager? = null

    private var channelId = "gotifyChannelID"

    /**
     * Handler of incoming messages from clients.
     */
    internal inner class gHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_NEW_URL -> if(checkGotifyId(msg.sendingUid)) newUrl(msg)
                MSG_NOTIFICATION -> if(checkGotifyId(msg.sendingUid)) showNotification(msg)
                else -> super.handleMessage(msg)
            }
        }
        //We only trust the app we registered to
        private fun checkGotifyId(id: Int): Boolean {
            val gotifyId = getSharedPreferences(PREF_GOTIFY, Context.MODE_PRIVATE)?.getInt(
                PREF_GOTIFY_KEY_ID,0)
            return (gotifyId == id)
        }
     }
    /** override it to handle newUrl */
    open fun newUrl(msg: Message){
    }
    /** override it if you want to custom your notifications */
    open fun showNotification(msg: Message) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        val text = msg.data?.getString("message")
        var title = msg.data?.getString("title")
        if(title.isNullOrBlank()){
            title = getText(R.string.app_name).toString()
        }
        var priority = msg.data!!.getInt("priority")
        // Set the info for the views that show in the notification panel.
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        }else{
            Notification.Builder(this)
        }
        val notification = notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round) // the status icon
            .setTicker(text) // the status text
            .setWhen(System.currentTimeMillis()) // the time stamp
            .setContentTitle(title) // the label of the entry
            .setContentText(text) // the contents of the entry
            .setPriority(priority)
            .build()

        gNM!!.notify(ThreadLocalRandom.current().nextInt(), notification)
    }
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId.isNotEmpty()) {
            val name = packageName
            val descriptionText = "gotify"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Target we publish for clients to send messages to gHandler.
     */
    private val gMessenger = Messenger(gHandler())

    override fun onCreate() {
        channelId = packageName
        createNotificationChannel()
        gNM = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    override fun onBind(intent: Intent): IBinder? {
        return gMessenger.binder
    }
}