package org.k40agent.automation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AgentState {
    var connected by mutableStateOf(false)
    var active by mutableStateOf(false)
    var status by mutableStateOf("待机")
    var logs by mutableStateOf(listOf<String>())
    fun log(message: String) {
        status = message
        val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date())
        logs = (listOf("$time  $message") + logs).take(80)
    }
}
