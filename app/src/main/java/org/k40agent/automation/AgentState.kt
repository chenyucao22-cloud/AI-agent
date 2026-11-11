package org.k40agent.automation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object AgentState {
    var status by mutableStateOf("待机；无需权限")
    var remaining by mutableIntStateOf(0)
    var logs by mutableStateOf(listOf<String>())
    fun log(message: String) { status = message; logs = (listOf(message) + logs).take(40) }
}
