package com.logflare.android.push

/** Joins the non-blank parts of a push notification body, one per line; absent parts are omitted. */
fun composePushBody(head: String, message: String?, at: String?): String =
    listOfNotNull(head, message, at).filter { it.isNotBlank() }.joinToString("\n")
