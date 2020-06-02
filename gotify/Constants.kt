package com.flyingpanda.gotificationtester.gotify

/**
 * Command to the service to register a client, receiving callbacks
 * from the service.  The Message's replyTo field must be a Messenger of
 * the client where callbacks should be sent.
 */
const val MSG_START = 1
const val MSG_REGISTER_CLIENT = 2
const val MSG_UNREGISTER_CLIENT = 3
const val MSG_NEW_URL = 4
const val MSG_NOTIFICATION = 5

const val PREF_GOTIFY = "gotify"
const val PREF_GOTIFY_KEY_ID = "gotifyId"
