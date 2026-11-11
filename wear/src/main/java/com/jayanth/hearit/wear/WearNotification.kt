package com.jayanth.hearit.wear

data class WearNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val message: String,
    val timestamp: Long
)
