package com.flyingpanda.gotificationtester.gotify

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.core.os.bundleOf

/**
 * This Activity is used to register to gotify
 */

var TOKEN = ""
var URL = ""

private const val gotify_package = "com.github.gotify"
private const val messenger_service = "$gotify_package.service.GotifyMessengerService"

fun logi(msg: String){
        Log.i("GotifyServiceBind",msg)
    }
fun logw(msg: String){
        Log.w("GotifyServiceBind",msg)
    }

open class GotifyServiceBinding : Activity() {
    /** Messenger for communicating with service.  */
    private var gService: Messenger? = null
    /** To known if it if bound to the service */
    private var gIsBound = false
    private var waitingForInfo = false

    /**
     * Handler of incoming messages from service.
     */
    internal open inner class gHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_START -> logi("Received MSG_START from service")
                MSG_REGISTER_CLIENT -> {
                    if(waitingForInfo) {
                        registerGotify(msg.sendingUid)
                        TOKEN = msg.data?.getString("token").toString()
                        URL = msg.data?.getString("url").toString()
                        logi("new token: $TOKEN")
                        logi("new url: $URL")
                    }
                }
                MSG_UNREGISTER_CLIENT -> logi("App is unregistered")
                else -> super.handleMessage(msg)
            }
        }
        private fun registerGotify(id: Int){
            // Trust only the app we registered to
            // We don't trust other apps
            // TODO: manually clear cache
            getSharedPreferences(PREF_GOTIFY, Context.MODE_PRIVATE).edit().putInt(PREF_GOTIFY_KEY_ID, id).commit()
            waitingForInfo = false
        }
    }

    /**
     * Target we publish for clients to send messages to gHandler.
     * open if the subclass want to add functions to the handler
     */
    open val gMessenger = Messenger(gHandler())

    /**
     * Class for interacting with the main interface of the service.
     */
    private val gConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            gService = Messenger(service)
            try {
                // Tell the service we have started
                val msg = Message.obtain(null,
                    MSG_START, 0, 0)
                msg.replyTo = gMessenger
                gService!!.send(msg)
            } catch (e: RemoteException) {
                // There is nothing special we need to do if the service
                // has crashed.
            }
            gIsBound = true
            logi("Remote service connected")
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            gService = null
            doUnbindService()
            logi("Remote service disconnected")
        }
    }

    fun doBindService() {
        val intent = Intent()
        intent.component = ComponentName(gotify_package , messenger_service)
        bindService( intent, gConnection, Context.BIND_AUTO_CREATE)
    }

    fun doUnbindService() {
        if (gIsBound) {
            // Detach our existing connection.
            unbindService(gConnection)
            gIsBound = false
        }
    }

    fun doRegisterApp(){
        if(!gIsBound){
            logw("You need to bind fisrt")
            return
        }
        try {
            val msg = Message.obtain(null,
                MSG_REGISTER_CLIENT, 0, 0)
            msg.replyTo = gMessenger
            msg.data = bundleOf("package" to packageName, "service" to serviceName)
            waitingForInfo = true
            gService!!.send(msg)
        } catch (e: RemoteException) {
            waitingForInfo = false
            // There is nothing special we need to do if the service
            // has crashed.
        }
    }

    fun doUnregisterApp(){
        if(!gIsBound){
            logw("You need to bind first")
            return
        }
        try {
            val msg = Message.obtain(null,
                MSG_UNREGISTER_CLIENT, 0, 0)
            msg.replyTo = gMessenger
            msg.data = bundleOf("package" to packageName)
            gService!!.send(msg)
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service
            // has crashed.
        }
    }
}