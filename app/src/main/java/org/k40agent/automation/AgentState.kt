package org.k40agent.automation
import androidx.compose.runtime.*
object AgentState {
 var connected by mutableStateOf(false)
 var active by mutableStateOf(false)
 var status by mutableStateOf("待机")
 var allowed by mutableStateOf(setOf("org.k40agent.controlled", "com.android.settings"))
 var logs by mutableStateOf(listOf<String>())
 fun log(message: String) { status = message; logs = (listOf(message) + logs).take(50) }
}
